package com.example.library.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.library.common.exception.BusinessException;
import com.example.library.common.result.ResultCode;
import com.example.library.dto.BookAddDTO;
import com.example.library.dto.BookQueryDTO;
import com.example.library.dto.BookUpdateDTO;
import com.example.library.entity.Book;
import com.example.library.entity.BookCategory;
import com.example.library.repository.BookRepository;
import com.example.library.service.BookCategoryService;
import com.example.library.service.BookService;
import com.example.library.util.RedisService;
import com.example.library.vo.BookVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 图书服务实现类
 *
 * <p>核心业务：
 * <ul>
 *   <li>分页查询：动态条件构建 + 排序 + 分页</li>
 *   <li>CRUD：新增、更新、逻辑删除</li>
 *   <li>库存管理：原子操作扣减/增加库存</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final BookCategoryService categoryService;
    private final RedisService redisService;

    /** 图书详情缓存 key 前缀 */
    private static final String BOOK_CACHE_PREFIX = "library:book:";
    /** 缓存过期时间（分钟） */
    private static final long CACHE_EXPIRE_MINUTES = 30;

    /** 允许排序的字段白名单（防止 SQL 注入） */
    private static final Set<String> ALLOWED_ORDER_FIELDS = Set.of(
            "id", "title", "author", "price", "stock", "create_time", "publish_date"
    );

    @Override
    public IPage<BookVO> pageBooks(BookQueryDTO queryDTO) {
        // 1. 构建分页对象
        Page<Book> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        // 2. 构建动态查询条件
        LambdaQueryWrapper<Book> wrapper = buildQueryWrapper(queryDTO);

        // 3. 执行分页查询
        IPage<Book> bookPage = bookRepository.page(page, wrapper);

        // 4. 转换为 VO（补充分类名称）
        return bookPage.convert(this::toVO);
    }

    /**
     * 构建动态查询条件
     */
    private LambdaQueryWrapper<Book> buildQueryWrapper(BookQueryDTO queryDTO) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();

        // 书名模糊查询
        if (StringUtils.hasText(queryDTO.getTitle())) {
            wrapper.like(Book::getTitle, queryDTO.getTitle());
        }

        // 作者模糊查询
        if (StringUtils.hasText(queryDTO.getAuthor())) {
            wrapper.like(Book::getAuthor, queryDTO.getAuthor());
        }

        // ISBN 精确查询
        if (StringUtils.hasText(queryDTO.getIsbn())) {
            wrapper.eq(Book::getIsbn, queryDTO.getIsbn());
        }

        // 分类ID
        if (queryDTO.getCategoryId() != null) {
            wrapper.eq(Book::getCategoryId, queryDTO.getCategoryId());
        }

        // 只查询有库存的图书
        if (Boolean.TRUE.equals(queryDTO.getOnlyAvailable())) {
            wrapper.gt(Book::getStock, 0);
        }

        // 排序（白名单校验）
        String orderField = queryDTO.getOrderBy();
        if (StringUtils.hasText(orderField) && ALLOWED_ORDER_FIELDS.contains(orderField)) {
            boolean isAsc = "asc".equalsIgnoreCase(queryDTO.getOrderDir());
            wrapper.orderBy(true, isAsc, getColumnFunction(orderField));
        } else {
            // 默认按创建时间倒序
            wrapper.orderByDesc(Book::getCreateTime);
        }

        return wrapper;
    }

    /**
     * 根据字段名获取对应的列引用（用于排序）
     */
    private com.baomidou.mybatisplus.core.toolkit.support.SFunction<Book, ?> getColumnFunction(String field) {
        return switch (field) {
            case "id" -> Book::getId;
            case "title" -> Book::getTitle;
            case "author" -> Book::getAuthor;
            case "price" -> Book::getPrice;
            case "stock" -> Book::getStock;
            case "publish_date" -> Book::getPublishDate;
            default -> Book::getCreateTime;
        };
    }

    @Override
    public BookVO getBookById(Long id) {
        String cacheKey = BOOK_CACHE_PREFIX + id;

        // 1. 先查缓存（缓存命中则直接返回，减少数据库压力）
        BookVO cachedBook = redisService.get(cacheKey, BookVO.class);
        if (cachedBook != null) {
            log.debug("图书详情缓存命中, bookId={}", id);
            return cachedBook;
        }

        // 2. 缓存未命中，查数据库
        Book book = bookRepository.getById(id);
        if (book == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }

        // 3. 转换为 VO 并写入缓存（设置过期时间，防止数据永久不一致）
        BookVO bookVO = toVO(book);
        redisService.set(cacheKey, bookVO, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        log.debug("图书详情写入缓存, bookId={}, expire={}分钟", id, CACHE_EXPIRE_MINUTES);

        return bookVO;
    }

    /**
     * 清除图书详情缓存（在新增、更新、删除时调用，保证缓存一致性）
     */
    private void evictBookCache(Long bookId) {
        try {
            redisService.delete(BOOK_CACHE_PREFIX + bookId);
            log.debug("清除图书缓存, bookId={}", bookId);
        } catch (Exception e) {
            // 缓存清除失败不影响主业务
            log.warn("清除图书缓存失败, bookId={}", bookId, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookVO addBook(BookAddDTO addDTO) {
        // 1. 校验 ISBN 是否已存在
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getIsbn, addDTO.getIsbn());
        if (bookRepository.count(wrapper) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "ISBN 已存在");
        }

        // 2. 校验分类是否存在
        if (addDTO.getCategoryId() != null) {
            BookCategory category = categoryService.getById(addDTO.getCategoryId());
            if (category == null) {
                throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
            }
        }

        // 3. 构建实体并保存
        Book book = new Book();
        BeanUtils.copyProperties(addDTO, book);
        book.setTotalStock(addDTO.getStock());  // 初始总藏书量 = 库存
        bookRepository.save(book);

        log.info("新增图书成功: bookId={}, title={}", book.getId(), book.getTitle());
        return toVO(book);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookVO updateBook(Long id, BookUpdateDTO updateDTO) {
        // 1. 校验图书是否存在
        Book existingBook = bookRepository.getById(id);
        if (existingBook == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }

        // 2. 校验分类是否存在
        if (updateDTO.getCategoryId() != null) {
            BookCategory category = categoryService.getById(updateDTO.getCategoryId());
            if (category == null) {
                throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND);
            }
        }

        // 3. 构建更新实体（只更新非空字段）
        Book book = new Book();
        book.setId(id);
        BeanUtils.copyProperties(updateDTO, book, "stock");

        // 4. 如果更新了库存，同时更新总藏书量的差值
        if (updateDTO.getStock() != null) {
            int diff = updateDTO.getStock() - existingBook.getStock();
            book.setStock(updateDTO.getStock());
            book.setTotalStock(existingBook.getTotalStock() + diff);
        }

        // 5. 执行更新
        bookRepository.updateById(book);

        // 6. 清除缓存（保证缓存一致性，下次查询时重新加载）
        evictBookCache(id);

        log.info("更新图书成功: bookId={}", id);
        return getBookById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBook(Long id) {
        Book book = bookRepository.getById(id);
        if (book == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
        // 逻辑删除（MyBatis-Plus 自动处理 deleted 字段）
        bookRepository.removeById(id);

        // 清除缓存
        evictBookCache(id);

        log.info("删除图书成功: bookId={}", id);
    }

    @Override
    public Book getById(Long id) {
        return bookRepository.getById(id);
    }

    @Override
    public int decreaseStock(Long bookId, Integer count) {
        return bookRepository.decreaseStock(bookId, count);
    }

    @Override
    public int increaseStock(Long bookId, Integer count) {
        return bookRepository.increaseStock(bookId, count);
    }

    @Override
    public BookVO toVO(Book book) {
        if (book == null) {
            return null;
        }
        BookVO vo = new BookVO();
        BeanUtils.copyProperties(book, vo);

        // 补充分类名称
        if (book.getCategoryId() != null) {
            BookCategory category = categoryService.getById(book.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }

        // 设置是否可借
        vo.setAvailable(book.getStock() != null && book.getStock() > 0);

        return vo;
    }
}

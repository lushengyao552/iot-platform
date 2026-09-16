package com.iot.platform.product.service;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iot.platform.common.exception.BusinessException;
import com.iot.platform.common.result.ResultCode;
import com.iot.platform.product.dto.ProductDTO;
import com.iot.platform.product.entity.Product;
import com.iot.platform.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;

    public Map<String, Object> page(int pageNum, int pageSize, String name) {
        Page<Product> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(Product::getName, name);
        }
        wrapper.orderByDesc(Product::getCreateTime);
        productMapper.selectPage(page, wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        return result;
    }

    public Product getById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        return product;
    }

    public void add(ProductDTO dto) {
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        product.setProductKey("PK" + RandomUtil.randomString(12).toUpperCase());
        product.setStatus(1);
        productMapper.insert(product);
    }

    public void update(Long id, ProductDTO dto) {
        Product product = getById(id);
        if (dto.getName() != null) product.setName(dto.getName());
        if (dto.getProtocol() != null) product.setProtocol(dto.getProtocol());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        productMapper.updateById(product);
    }

    public void delete(Long id) {
        productMapper.deleteById(id);
    }
}

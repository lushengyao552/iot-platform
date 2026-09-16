package com.iot.platform.device.service;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iot.platform.common.exception.BusinessException;
import com.iot.platform.common.result.ResultCode;
import com.iot.platform.device.dto.DeviceRegisterDTO;
import com.iot.platform.device.entity.Device;
import com.iot.platform.device.mapper.DeviceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceMapper deviceMapper;
    private final StringRedisTemplate redisTemplate;

    private static final String ONLINE_KEY = "device:online:";

    public Map<String, Object> page(int pageNum, int pageSize, String productKey, String status) {
        Page<Device> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        if (productKey != null && !productKey.isEmpty()) {
            wrapper.eq(Device::getProductKey, productKey);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Device::getStatus, status);
        }
        wrapper.orderByDesc(Device::getCreateTime);
        deviceMapper.selectPage(page, wrapper);

        // 补充 Redis 在线状态
        page.getRecords().forEach(d -> {
            Boolean online = redisTemplate.hasKey(ONLINE_KEY + d.getDeviceName());
            d.setStatus(Boolean.TRUE.equals(online) ? "ONLINE" : "OFFLINE");
        });

        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        return result;
    }

    public Device getById(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) throw new BusinessException(ResultCode.DEVICE_NOT_FOUND);
        return device;
    }

    public Map<String, Object> register(DeviceRegisterDTO dto) {
        Device device = new Device();
        device.setProductKey(dto.getProductKey());
        device.setDeviceName(dto.getDeviceName() != null ? dto.getDeviceName() : "DEV" + RandomUtil.randomString(10).toUpperCase());
        device.setDeviceSecret(RandomUtil.randomString(32));
        device.setStatus("INACTIVE");
        deviceMapper.insert(device);

        Map<String, Object> result = new HashMap<>();
        result.put("deviceName", device.getDeviceName());
        result.put("deviceSecret", device.getDeviceSecret());
        return result;
    }

    public void online(String deviceName, String ip) {
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceName, deviceName));
        if (device == null) throw new BusinessException(ResultCode.DEVICE_NOT_FOUND);

        device.setStatus("ONLINE");
        device.setLastIp(ip);
        device.setLastOnlineTime(LocalDateTime.now());
        deviceMapper.updateById(device);

        redisTemplate.opsForValue().set(ONLINE_KEY + deviceName, "1", 90, TimeUnit.SECONDS);
    }

    public void heartbeat(String deviceName) {
        redisTemplate.opsForValue().set(ONLINE_KEY + deviceName, "1", 90, TimeUnit.SECONDS);
    }

    public void offline(String deviceName) {
        redisTemplate.delete(ONLINE_KEY + deviceName);
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceName, deviceName));
        if (device != null) {
            device.setStatus("OFFLINE");
            deviceMapper.updateById(device);
        }
    }
}

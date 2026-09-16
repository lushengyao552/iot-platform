package com.iot.platform.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.platform.device.entity.Device;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DeviceMapper extends BaseMapper<Device> {
}

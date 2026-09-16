package com.iot.platform.alarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.platform.alarm.entity.AlarmRule;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AlarmRuleMapper extends BaseMapper<AlarmRule> {
}

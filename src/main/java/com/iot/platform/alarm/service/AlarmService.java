package com.iot.platform.alarm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iot.platform.alarm.entity.AlarmRule;
import com.iot.platform.alarm.mapper.AlarmRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlarmService {

    private final AlarmRuleMapper alarmRuleMapper;

    public List<AlarmRule> list(String productKey) {
        LambdaQueryWrapper<AlarmRule> wrapper = new LambdaQueryWrapper<>();
        if (productKey != null) wrapper.eq(AlarmRule::getProductKey, productKey);
        return alarmRuleMapper.selectList(wrapper);
    }

    public void create(AlarmRule rule) {
        alarmRuleMapper.insert(rule);
    }

    public void checkAlarm(String productKey, Map<String, Object> metrics) {
        List<AlarmRule> rules = alarmRuleMapper.selectList(
                new LambdaQueryWrapper<AlarmRule>().eq(AlarmRule::getProductKey, productKey));

        for (AlarmRule rule : rules) {
            Object value = metrics.get(rule.getMetric());
            if (value == null) continue;

            double num = Double.parseDouble(value.toString());
            boolean triggered = switch (rule.getOperator()) {
                case ">" -> num > rule.getThreshold();
                case "<" -> num < rule.getThreshold();
                case "=" -> num == rule.getThreshold();
                default -> false;
            };

            if (triggered) {
                log.warn("告警触发: rule={}, device metric={}, value={}, threshold={}",
                        rule.getRuleName(), rule.getMetric(), num, rule.getThreshold());
            }
        }
    }
}

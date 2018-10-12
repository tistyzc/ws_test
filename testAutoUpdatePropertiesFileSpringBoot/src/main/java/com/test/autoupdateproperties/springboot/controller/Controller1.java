package com.test.autoupdateproperties.springboot.controller;

import com.test.autoupdateproperties.springboot.config.GeekProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试用的控制器
 *
 * @author yuzc
 * @date 20181012
 */
@RestController
@RequestMapping("/api/server/")
@Slf4j
public class Controller1 {
    @Autowired
    GeekProperties geekProperties;

    @RequestMapping(path = "/properties", method = {RequestMethod.GET})
    @ResponseBody
    public Object start() {
        Map<String, Object> map = new HashMap<>(8);
        map.put("channels", geekProperties.getChannels());
        map.put("Idle-microseconds", geekProperties.getIdleMicroseconds());
        return map;
    }
}

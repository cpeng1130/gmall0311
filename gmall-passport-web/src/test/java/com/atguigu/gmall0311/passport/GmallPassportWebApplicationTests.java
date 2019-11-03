package com.atguigu.gmall0311.passport;

import com.atguigu.gmall0311.passport.config.JwtUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallPassportWebApplicationTests {

	@Test
	public void contextLoads() {
	}

	@Test
	public void testJWT(){
		String key = "atguigu234565werasdfaweorqwrqrwerwrwesdfaf";
		HashMap<String, Object> map = new HashMap<>();
		map.put("userId","111");
		map.put("nickName","Administrator");
		String salt = "192.168.67.220";
		String token = JwtUtil.encode(key, map, salt);
		System.out.println("token:"+token);

		// 解密的出来的是用户信息
		Map<String, Object> objectMap = JwtUtil.decode(token, key, "192.168.34.19");
		System.out.println(objectMap);
//		System.out.println(objectMap.get("userId"));
//		System.out.println(objectMap.get("nickName"));

	}
}

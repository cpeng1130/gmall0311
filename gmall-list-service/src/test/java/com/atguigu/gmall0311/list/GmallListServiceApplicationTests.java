package com.atguigu.gmall0311.list;

import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallListServiceApplicationTests {

	@Autowired
	private JestClient jestClient;

	@Test
	public void contextLoads() {
	}

	@Test
	public void testES() throws IOException {
		/*
		1.	定义dsl 语句
		2.	定义要执行的动作
		3.	jestClient执行动作
		4.	获取返回的结果
		 */

		String query = "{\n" +
				"  \"query\": {\n" +
				"    \"match\": {\n" +
				"      \"name\": \"行动\"\n" +
				"    }\n" +
				"  }\n" +
				"}";

		Search search = new Search.Builder(query).addIndex("movie_chn").addType("movie").build();

		SearchResult searchResult = jestClient.execute(search);

		List<SearchResult.Hit<Map, Void>> hits = searchResult.getHits(Map.class);

		// 循环遍历集合
		for (SearchResult.Hit<Map, Void> hit : hits) {
			Map map = hit.source;
			String name = (String) map.get("name");
			System.out.println(name); // 红海行动 湄公河行动
		}


	}

}

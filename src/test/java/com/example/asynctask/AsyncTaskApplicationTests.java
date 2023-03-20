package com.example.asynctask;

import cn.hutool.core.collection.ListUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.NumberUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@SpringBootTest
class AsyncTaskApplicationTests {

    /**
     * <p>异步递归调用 每一次异步处理 依赖于上一步的结果递归调用</p>
     * <p>为了模拟Mongodb ID范围查询 为解决 百万数据量或大数据量分页 skip limit 过大导致查询性能特差的现象</p>
     * <p>可参考依据ElasticSearch的Scroll_After模式的概念优化自己的程序</p>
     * <p>此测试用例代表一次性将所有数据结果加载到JVM内容中</p>
     * <p>Restful API 接口可依据每次查询 倒序排查后取到第一个值的ID发送到Response中，下次查询将last_id传到后端接口中</p>
     */
    @Test
    void contextLoads() {

        List<Integer> list = new ArrayList<>();

        for (int i = 1; i <= 1000; i++) {
            list.add(i);
        }

        Integer integer = list.stream()
                .limit(10).sorted(Comparator.reverseOrder())
                .findFirst().get();

        List<List<Integer>> result = new ArrayList<>();

        CompletableFuture<Integer> integerCompletableFuture = CompletableFuture.supplyAsync(() -> {
            List<Integer> integers = new ArrayList<>();
            integers.addAll(list.subList(0, integer));
            result.add(integers);
            return integer;
        });

        Integer join = recursion(integerCompletableFuture, list, result).join();

        if (join == list.size()) {
            System.out.println("处理完成");
            // TODO
        }
    }

    private CompletableFuture<Integer> recursion(CompletableFuture<Integer> future, List<Integer> source, List<List<Integer>> out) {

        CompletableFuture<Integer> integerCompletableFuture = future.thenApply(i -> {
            List<Integer> collect = source.stream().filter(item -> item > i && item <= i + 10)
                    .collect(Collectors.toList());

            System.out.println(collect);

            try {
                // 模拟数据IO
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            out.add(collect);
            return collect.stream()
                    .limit(10).sorted(Comparator.reverseOrder())
                    .findFirst()
                    .get();
        });

        if (out.size() == 100) {
            return integerCompletableFuture;
        }
        return recursion(integerCompletableFuture, source, out);
    }
}

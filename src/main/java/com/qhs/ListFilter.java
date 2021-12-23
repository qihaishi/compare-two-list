package com.qhs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 求两个泛型集合差异（实质算法为集合循环比较，元素量过大慎用）
 * beforeList       (db中的数据)
 * afterList        (当前数据，前端传过来的)
 * keyMapperList    (连接条件)
 * valueMapperList  (需要比较内容的字段列表)
 * @param <T>
 */
public class ListFilter<T> {

    private List<T> beforeList;
    private List<T> afterList;
    private List<Function<T, Object>> keyMapperList = new ArrayList<>();
    private List<Function<T, Object>> valueMapperList = new ArrayList<>();
    private boolean calculateUpdate = true;
    private boolean calculateInsert = false;
    private boolean calculateDelete = false;
    private List<T> insertList = Collections.emptyList();
    private List<T> updateList = Collections.emptyList();
    private List<T> deleteList = Collections.emptyList();

    public static <T> ListFilter<T> builder(Class<T> cls) {
        return new ListFilter();
    }

    public ListFilter() {
    }

    public ListFilter<T> beforeList(List<T> beforeList) {
        this.beforeList = beforeList;
        return this;
    }

    public ListFilter<T> afterList(List<T> afterList) {
        this.afterList = afterList;
        return this;
    }

    public ListFilter<T> addKey(Function<T, Object> fn) {
        this.getKeyMapperList().add(fn);
        return this;
    }

    public ListFilter<T> addValue(Function<T, Object> fn) {
        this.getValueMapperList().add(fn);
        return this;
    }

    public ListFilter<T> insert(boolean b) {
        this.calculateInsert = b;
        return this;
    }

    public ListFilter<T> update(boolean b) {
        this.calculateUpdate = b;
        return this;
    }

    public ListFilter<T> delete(boolean b) {
        this.calculateDelete = b;
        return this;
    }

    public ListFilter<T> all() {
        this.calculateDelete = true;
        this.calculateInsert = true;
        return this;
    }

    public ListFilter<T> build() {
        this.exec();
        return this;
    }

    public static void main(String[] args) {
        List<User> list1 = new ArrayList<>();
        list1.add(new User(1, "张三"));
        list1.add(new User(2, "李四"));
        List<User> list2 = new ArrayList<>();
        list2.add(new User(1, "张三asd"));
        list2.add(new User(3, "王五"));
        long start = System.currentTimeMillis();
        ListFilter<User> result = ListFilter.builder(User.class)
                .beforeList(list1).afterList(list2)
                .addKey(User::getUserId).addValue(User::getUserName).all().build();
        System.out.println("cost:" + (System.currentTimeMillis() - start));
        System.out.println("新增：");
        result.getInsertList().forEach(System.out::println);
        System.out.println("更新：");
        result.getUpdateList().forEach(System.out::println);
        System.out.println("删除：");
        result.getDeleteList().forEach(System.out::println);
    }

    public void exec() {
        boolean beforeIsEmpty = beforeList == null || beforeList.isEmpty();
        boolean afterIsEmpty = afterList == null || afterList.isEmpty();

        if (beforeIsEmpty && afterIsEmpty) {
            return;
        }

        // 添加操作
        if (beforeIsEmpty) {
            insertList = afterList;
        }

        // 清空
        if (afterList == null || afterList.isEmpty()) {
            deleteList = beforeList;
        }

        // 计算更新
        if (calculateUpdate) {
            Predicate<T> predicateKey = e -> {
                Predicate<T> keyPredicate = c -> keyMapperList.stream().allMatch(p -> Objects.equals(p.apply(e), p.apply(c)));
                Predicate<T> valuePredicate = c -> valueMapperList.stream().allMatch(p -> !Objects.equals(p.apply(e), p.apply(c)));
                Predicate<T> key = Stream.of(keyPredicate, valuePredicate).reduce(x -> true, Predicate::and);
                return beforeList.stream().anyMatch(key);
            };
            updateList = afterList.stream().filter(predicateKey).collect(Collectors.toList());
        }

        // 计算新增
        if (calculateInsert) {
            Predicate<T> predicateKey = e -> beforeList.stream().noneMatch(c -> keyMapperList.stream().anyMatch(p -> Objects.equals(p.apply(e), p.apply(c))));
            insertList = afterList.stream().filter(predicateKey).collect(Collectors.toList());
        }

        // 计算删除
        if (calculateDelete) {
            Predicate<T> predicateKey = e -> afterList.stream().noneMatch(c -> keyMapperList.stream().allMatch(p -> Objects.equals(p.apply(e), p.apply(c))));
            deleteList = beforeList.stream().filter(predicateKey).collect(Collectors.toList());
        }
    }

    public List<Function<T, Object>> getKeyMapperList() {
        return keyMapperList;
    }

    public List<Function<T, Object>> getValueMapperList() {
        return valueMapperList;
    }

    public List<T> getDeleteList() {
        return deleteList;
    }

    public List<T> getUpdateList() {
        return updateList;
    }

    public List<T> getInsertList() {
        return insertList;
    }


    public static class User {
        private Integer userId;
        private String userName;

        public User(Integer userId, String userName) {
            this.userId = userId;
            this.userName = userName;
        }

        @Override
        public String toString() {
            return "User{" +
                    "userId=" + userId +
                    ", userName='" + userName + '\'' +
                    '}';
        }

        public User() {
        }

        public Integer getUserId() {
            return userId;
        }

        public void setUserId(Integer userId) {
            this.userId = userId;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }
    }
}

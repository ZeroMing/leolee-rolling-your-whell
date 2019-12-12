package org.leolee.wheel.cache;

import javax.xml.soap.Node;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 最简单版本的LRU策略下的Cache
 * @author: LeoLee <zeroming@163.com>
 * @date: 2019/12/12 14:38
 * @since:
 */
public class LRUCache_V1<K,V> {

    /**
     * 目标:
     * 1. 根据key快速存取,时间复杂度最好达到O(1)
     * 2. 元素达到，缓存容量上限之后，触发数据淘汰机制。删除符合淘汰策略的元素。时间复杂度最好为O(1)
     * 3. 内存条件充足，不算苛刻。
     * 4. 扩展: 如果元素默认带有过期时间,如何实现? 参考Redis的实现
     *
     * 实现思路概要:
     *
     * 1. 通过Hash表结构存映射关系。使用HashMap或者ConcurrentHashMap（并发性更好）.
     * 使用双向链表，存储数据LRU顺序。
     * 使用双向链表 DoubleLinkedNode，存储数据LRU顺序。
     *
     * 2. 存。判断Map中是否存在指定key，
     *  2.1. 如果存在，更新Map中对应的value。将该元素从LRU中移除，然后添加到head。
     *  2.2. 如果不存在，创建新的节点数据，添加到Map中。判断LRU队列的大小是否超过队列容量，
     *   2.2.1. 如果超过，将tail元素移除，将新的节点添加到head。
     *   2.2.2. 如果不超过，只需要将新的几节点添加到head。
     *
     * 3. 取。Map中如果不存在元素，返回null.如果取到元素，需要将该元素从LRU中移除，然后添加到head位置
     *
     * 4. 带有过期时间的元素。需要单独开启一个cron任务，用来轮询处理失效时间到期的数据。
     *
     *
     *
     * 思考:
     * 1. 如果内存资源有限，需要尽可能少的占用内存，如何实现更好？
     *
     *
     */
    private int count;
    // 缓存的最大容量
    private int capacity;
    // 哨兵节点
    private DoubleLinkedNode head, tail;
    // 缓存
    private Map<K, DoubleLinkedNode<K, V>> cache = new ConcurrentHashMap<>();

    public LRUCache_V1(int capacity){
        this.count = 0;
        this.capacity = capacity;
        this.head = new DoubleLinkedNode();
        this.tail = new DoubleLinkedNode();
        head.next = tail;
        tail.prev = head;
    }


    public DoubleLinkedNode<K,V> put(K key,V value){
        DoubleLinkedNode<K,V> node = cache.get(key);
        if(node != null){
            node.value = value;
            // 将node移动至head
            this.moveToHead(node);
            return node;
        }else{
            DoubleLinkedNode<K,V> newNode = new DoubleLinkedNode();
            newNode.key = key;
            newNode.value = value;
            cache.put(key,newNode);
            this.addNode(newNode);
            count ++;
            // 大于容量，进行淘汰
            if(count > capacity){
                DoubleLinkedNode tailNode = this.popTail();
                cache.remove(tailNode.key);
                count --;
            }
            return newNode;
        }
    }


    /**
     * 添加头节点
     * @param node
     */
    private void addNode(DoubleLinkedNode<K,V> node){
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }

    /**
     * 移除尾节点
     * @return
     */
    private DoubleLinkedNode<K,V> popTail(){
        DoubleLinkedNode<K,V> res = tail.prev;
        this.removeNode(res);
        return res;
    }




    //  head

    /**
     *
     * a - b - c
     * 移除一个链表节点
     * @param node
     */
    public void removeNode(DoubleLinkedNode<K,V> node){
        DoubleLinkedNode<K,V> prevNode = node.prev;
        DoubleLinkedNode<K,V> nextNode = node.next;
        prevNode.next = nextNode;
        nextNode.prev = prevNode;
        node.prev = null;
        node.next = null;
    }

    /**
     * 将链表节点移动至队头.
     * 先从原位置移除，添加到队头
     * @param node
     */
    public void moveToHead(DoubleLinkedNode<K,V> node){
        this.removeNode(node);
        this.addNode(node);
    }



    /**
     * 获取节点
     * @param key
     * @return
     */
    public  V get(K key){
        DoubleLinkedNode<K,V> node =cache.get(key);
        if(node != null){
            this.moveToHead(node);
        }
        return node.value;
    }


    class DoubleLinkedNode<K,V>{
        K key;
        V value;
        DoubleLinkedNode<K,V> prev;
        DoubleLinkedNode<K,V> next;


    }

    public void  print(){
        DoubleLinkedNode node = head.next;
        while (node != null && node.value != null){
            System.out.println(node.key +":"+node.value);
            node = node.next;
        }
        System.out.println();
    }



    public static void main(String[] args) {

        LRUCache_V1<Integer,String> cache = new LRUCache_V1(3);
        cache.put(1,"1-1");
        cache.put(2,"2-2");
        cache.put(3,"3-3");
        cache.print();

        System.out.println();

        cache.put(4,"4-4");
        cache.print();

        System.out.println();

        cache.put(5,"5-5");
        cache.print();


        cache.get(3);
        cache.print();
    }




}

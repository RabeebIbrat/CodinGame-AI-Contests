import java.util.*;

public class Test {
    public static void main(String[] args) {
        PriorityQueue<Integer> q = new PriorityQueue<>(Collections.reverseOrder());
        q.offer(3);
        q.offer(5);
        q.offer(7);
        q.offer(5);
        q.offer(3);

        while(!q.isEmpty())
            System.out.println(q.poll());
    }
}

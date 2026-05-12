package timingtest;
import edu.princeton.cs.algs4.Stopwatch;

/**
 * Created by hug.
 */
public class TimeSLList {
    private static void printTimingTable(AList<Integer> Ns, AList<Double> times, AList<Integer> opCounts) {
        System.out.printf("%12s %12s %12s %12s\n", "N", "time (s)", "# ops", "microsec/op");
        System.out.printf("------------------------------------------------------------\n");
        for (int i = 0; i < Ns.size(); i += 1) {
            int N = Ns.get(i);
            double time = times.get(i);
            int opCount = opCounts.get(i);
            double timePerOp = time / opCount * 1e6;
            System.out.printf("%12d %12.2f %12d %12.2f\n", N, time, opCount, timePerOp);
        }
    }

    public static void main(String[] args) {
        timeGetLast();
    }

    public static void timeGetLast() {
        // TODO: YOUR CODE HERE
        AList<Integer> Ns = createNsList();
        AList<Double> times = new AList<>();
        AList<Integer> opCounts = new AList<>();

        int m = 10000;

        for (int i = 0; i < Ns.size(); i++) {
            SLList<Integer> processSLList = processSLList(Ns.get(i));
            Stopwatch sp = new Stopwatch();
            callGetLast(processSLList, m);
            times.addLast(sp.elapsedTime());
            opCounts.addLast(m);
        }

        printTimingTable(Ns, times, opCounts);

    }

    private static AList<Integer> createNsList() {
        AList<Integer> Ns = new AList<>();
        int n = 1000;
        while (n <= 128000) {
            Ns.addLast(n);
            n *= 2;
        }
        return Ns;
    }
    private static SLList<Integer> processSLList(int n) {
        SLList<Integer> Ns = new SLList<>();
        for (int i = 0; i < n; i++) {
            Ns.addLast(1);
        }
        return Ns;
    }

    private static void callGetLast(SLList<Integer> slList, int m) {
        for (int i = 0; i < m; i++) {
            slList.getLast();
        }
    }
}

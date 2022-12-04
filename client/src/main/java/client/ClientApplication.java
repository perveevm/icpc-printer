package client;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClientApplication {
    public static void main(String[] args) {
        if (args == null) {
            System.out.println("Arguments cannot be null");
            return;
        }
        if (args.length != 2) {
            System.out.println("There should be two arguments: URL and token");
            return;
        }

        PrinterClient client = new PrinterClient(args[0], args[1]);
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleWithFixedDelay(client::checkQueueAndPrint, 0, 5, TimeUnit.SECONDS);
    }
}

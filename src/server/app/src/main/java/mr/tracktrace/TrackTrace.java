package mr.tracktrace;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class TrackTrace {
    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new TrackTraceModule());
        Service service = injector.getInstance(Service.class);

        service.start();
    }
}

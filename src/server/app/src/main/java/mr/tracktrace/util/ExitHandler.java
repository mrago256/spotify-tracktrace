package mr.tracktrace.util;

import com.google.inject.Singleton;

@Singleton
public class ExitHandler {
    public void exit(int statusCode) {
        System.exit(statusCode);
    }
}

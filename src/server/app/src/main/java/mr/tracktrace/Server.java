package mr.tracktrace;

import mr.tracktrace.adapter.SongTableDynamoAdapter;

public class Server {
    private final SongTableDynamoAdapter songTableDynamoAdapter;

    public Server(SongTableDynamoAdapter songTableDynamoAdapter) {
        this.songTableDynamoAdapter = songTableDynamoAdapter;
    }

    public void run() {
        System.out.println(songTableDynamoAdapter.test());
    }
}

package core.records;

import java.util.ArrayList;
import java.util.List;

public record Channel(String name, int users, String options, String desc, List<Message> messages) {
    public Channel(String name) {
        this(name, 0, null, null, new ArrayList<>());
    }
    
    public Channel(String name, int users, String options, String desc) {
        this(name, users, options, desc, new ArrayList<>());
    }
}

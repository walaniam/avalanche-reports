package walaniam.avalanches.regions;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Region implements Comparable<Region> {
    private String id;
    private String name;

    @Override
    public int compareTo(Region o) {
        return name.compareTo(o.name);
    }
}

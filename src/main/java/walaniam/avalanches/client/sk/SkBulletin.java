package walaniam.avalanches.client.sk;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Represents a single CAAMLv5 Bulletin parsed from a Slovak avalanche report.
 */
@Getter
@AllArgsConstructor
public class SkBulletin {

    private final Set<String> regionIds;
    private final LocalDateTime beginPosition;
    private final LocalDateTime endPosition;
    private final List<Integer> dangerRatings;
    private final String highlights;
    private final String comment;
    private final String snowpackStructureComment;

    /**
     * Returns the maximum danger rating among all elevation bands for this bulletin.
     */
    public int getMaxDangerRating() {
        return dangerRatings.stream()
            .mapToInt(Integer::intValue)
            .max()
            .orElse(1);
    }
}

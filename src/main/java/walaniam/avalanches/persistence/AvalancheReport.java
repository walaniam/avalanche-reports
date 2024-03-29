package walaniam.avalanches.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class AvalancheReport {

    private ReportId id;
    private int avalancheLevel;
    private LocalDateTime reportDate;
    private LocalDateTime reportExpirationDate;
    private String comment;
}

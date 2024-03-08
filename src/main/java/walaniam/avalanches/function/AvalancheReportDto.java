package walaniam.avalanches.function;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class AvalancheReportDto {

    private int avalancheLevel;
    private String reportedBy;
    private String reportDate;
    private String reportExpirationDate;
    private String comment;
}

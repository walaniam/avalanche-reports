package walaniam.avalanches.persistence;

import lombok.*;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class BinaryReport {
    @ToString.Include
    private ReportId id;
    private byte[] bytes;
    private String contentType;
    @ToString.Include
    private LocalDate day;
}

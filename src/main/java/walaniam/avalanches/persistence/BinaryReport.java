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
    @ToString.Include
    private String contentType;
    private LocalDate day;
}

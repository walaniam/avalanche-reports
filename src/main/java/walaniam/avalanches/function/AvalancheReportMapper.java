package walaniam.avalanches.function;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import walaniam.avalanches.persistence.AvalancheReport;

@Mapper
public interface AvalancheReportMapper {

    AvalancheReportMapper INSTANCE = Mappers.getMapper(AvalancheReportMapper.class);

    AvalancheReportDto toDataView(AvalancheReport data);
}

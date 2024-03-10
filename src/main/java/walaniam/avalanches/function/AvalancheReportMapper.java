package walaniam.avalanches.function;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import walaniam.avalanches.persistence.AvalancheReport;

@Mapper
public interface AvalancheReportMapper {

    AvalancheReportMapper INSTANCE = Mappers.getMapper(AvalancheReportMapper.class);

    @Mapping(target = "reportedBy", source = "id.reportedBy")
    AvalancheReportDto toDataView(AvalancheReport data);
}

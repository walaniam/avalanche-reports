package walaniam.avalanches.function;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import walaniam.avalanches.persistence.AvalancheReport;
import walaniam.avalanches.regions.Region;
import walaniam.avalanches.regions.RegionsMapper;

@Mapper
public abstract class AvalancheReportMapper {

    static final AvalancheReportMapper INSTANCE = Mappers.getMapper(AvalancheReportMapper.class);
    private static final RegionsMapper REGIONS_MAPPER = new RegionsMapper();

    @Mapping(target = "reportedBy", source = "id.reportedBy")
    @Mapping(target = "region", source = "id.regionId", qualifiedByName = "mapRegion")
    abstract AvalancheReportDto toDataView(AvalancheReport data);

    @Named("mapRegion")
    Region mapRegion(String regionId) {
        String regionName = REGIONS_MAPPER.getRegionName(regionId).orElse(regionId);
        return Region.builder()
                .id(regionId)
                .name(regionName)
                .build();
    }
}

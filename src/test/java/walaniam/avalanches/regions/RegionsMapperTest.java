package walaniam.avalanches.regions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class RegionsMapperTest {

    private RegionsMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new RegionsMapper();
    }

    @Test
    void shouldReturnRegionNameById() {
        Optional<String> name = mapper.getRegionName("PL");

        assertThat(name).isPresent();
        assertThat(name.get()).isNotBlank();
    }

    @Test
    void shouldReturnEmptyOptionalForUnknownId() {
        Optional<String> name = mapper.getRegionName("UNKNOWN-REGION-XYZ");

        assertThat(name).isEmpty();
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenJsonIsInvalid() {
        // Uses a test resource containing invalid JSON to verify error handling
        assertThatThrownBy(() -> new RegionsMapper("/walaniam/avalanches/regions/invalid_regions.json"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to load regions");
    }

    @Test
    void shouldCountRegions() {
        assertThat(mapper.getRegionsCount()).isEqualTo(703);
    }
}

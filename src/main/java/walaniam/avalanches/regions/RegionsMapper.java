package walaniam.avalanches.regions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Log
public class RegionsMapper {

    private static final String REGIONS_JSON_PATH = "/regions_pl.json";
    static final TypeReference<Map<String, String>> MAP_TYPE_REF = new TypeReference<>() {};

    private final Map<String, String> regionsById;

    public RegionsMapper() {
        this(REGIONS_JSON_PATH);
    }

    RegionsMapper(String resourcePath) {
        this.regionsById = loadRegions(resourcePath);
        log.info("Loaded " + regionsById.size() + " regions from " + resourcePath);
    }

    private static Map<String, String> loadRegions(String resourcePath) {
        try (InputStream is = RegionsMapper.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.warning("Regions resource not found: " + resourcePath);
                return Collections.emptyMap();
            }
            ObjectMapper objectMapper = new ObjectMapper();
            return Collections.unmodifiableMap(objectMapper.readValue(is, MAP_TYPE_REF));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load regions from " + resourcePath, e);
        }
    }

    /**
     * Returns the name of the region for the given id, or an empty Optional if not found.
     */
    public Optional<String> getRegionName(String regionId) {
        return Optional.ofNullable(regionsById.get(regionId));
    }

    public Set<String> getAllIds() {
        return regionsById.keySet();
    }

    int getRegionsCount() {
        return regionsById.size();
    }

}

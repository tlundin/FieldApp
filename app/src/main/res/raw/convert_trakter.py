import json
import math

# Constants for coordinate conversion (WGS84)
KM_PER_DEG_LAT = 111.32  # Approximate km per degree latitude
HALF_SIDE_KM = 2.5  # Half of 5000 meters

def point_to_square_polygon(lon, lat):
    """Create a 20km square polygon centered on (lon, lat)."""
    lat_offset = HALF_SIDE_KM / KM_PER_DEG_LAT
    lon_offset = HALF_SIDE_KM / (KM_PER_DEG_LAT * math.cos(math.radians(lat)))
    
    # Square corners: NW, NE, SE, SW, back to NW (closed ring)
    coordinates = [
        [lon - lon_offset, lat + lat_offset],  # NW
        [lon + lon_offset, lat + lat_offset],  # NE
        [lon + lon_offset, lat - lat_offset],  # SE
        [lon - lon_offset, lat - lat_offset],  # SW
        [lon - lon_offset, lat + lat_offset],  # Close ring
    ]
    return coordinates

# Read input
with open('trakter.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

# Convert each point to a polygon
new_features = []
for feature in data['features']:
    if feature['geometry']['type'] != 'Point':
        continue
    coords_raw = feature['geometry']['coordinates']
    lon, lat = coords_raw[0], coords_raw[1]
    coords = point_to_square_polygon(lon, lat)
    
    new_feature = {
        'type': 'Feature',
        'geometry': {
            'type': 'Polygon',
            'coordinates': [coords]
        },
        'properties': feature['properties'].copy()
    }
    new_features.append(new_feature)

# Build output
output = {
    'type': 'FeatureCollection',
    'name': data.get('name', 'Original_trakterPY') + '_polygons',
    'crs': data.get('crs', {}),
    'features': new_features
}

# Write output
with open('trakter_poly.json', 'w', encoding='utf-8') as f:
    json.dump(output, f, indent=2, ensure_ascii=False)

print(f"Created trakter_poly.json with {len(new_features)} polygon features")

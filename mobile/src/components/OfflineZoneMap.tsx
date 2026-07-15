import React, { useMemo } from "react";
import { StyleSheet, Text, View } from "react-native";
import Svg, { Circle, G, Path, Polygon as SvgPolygon } from "react-native-svg";

import { useRadiation } from "@/context/RadiationContext";
import { useAppTheme } from "@/theme/ThemeContext";
import { Zone } from "@/types";
import { getStatusColor } from "@/utils/radiation";

const WIDTH = 320;
const HEIGHT = 500;
const BOUNDS = { minLon: 43.35, maxLon: 46.75, minLat: 38.75, maxLat: 41.35 };

// Упрощённый локальный контур Армении. Он намеренно схематичный: карта
// служит для автономной визуализации зон, а не для навигации.
const ARMENIA_PATH =
  "M98 28 L142 48 L169 91 L216 109 L238 151 L225 193 L267 231 L251 278 L275 323 L239 359 L224 407 L183 470 L145 451 L127 399 L91 373 L79 329 L48 293 L66 252 L43 210 L68 170 L57 122 L88 87 Z";

function project(latitude: number, longitude: number) {
  return {
    x: ((longitude - BOUNDS.minLon) / (BOUNDS.maxLon - BOUNDS.minLon)) * WIDTH,
    y: ((BOUNDS.maxLat - latitude) / (BOUNDS.maxLat - BOUNDS.minLat)) * HEIGHT,
  };
}

function zonePolygon(zone: Zone, tick: number): string {
  const center = project(zone.centerLat, zone.centerLon);
  const severity = zone.status === "critical" ? 1 : zone.status === "dangerous" ? 0.75 : zone.status === "elevated" ? 0.55 : 0.38;
  const radius = 24 + severity * 32;
  return Array.from({ length: 8 }, (_, index) => {
    const angle = (Math.PI * 2 * index) / 8;
    const wobble = 0.86 + ((Math.sin(tick * 0.8 + index * 2.7 + zone.id.length) + 1) / 2) * 0.24;
    return `${center.x + Math.cos(angle) * radius * wobble},${center.y + Math.sin(angle) * radius * wobble}`;
  }).join(" ");
}

interface Props {
  zones: Zone[];
  tick: number;
  onZonePress: (zone: Zone) => void;
  pointerEventsNone?: boolean;
}

export const OfflineZoneMap: React.FC<Props> = ({ zones, tick, onZonePress, pointerEventsNone }) => {
  const { colors } = useAppTheme();
  const { snapshot } = useRadiation();
  const shapes = useMemo(() => zones.map((zone) => ({ zone, points: zonePolygon(zone, tick) })), [zones, tick]);

  return (
    <View style={[styles.container, { backgroundColor: colors.backgroundSecondary }]} pointerEvents={pointerEventsNone ? "none" : "auto"}>
      <Svg width="100%" height="100%" viewBox={`0 0 ${WIDTH} ${HEIGHT}`}>
        <Path d={ARMENIA_PATH} fill={colors.cardSolid} stroke={colors.border} strokeWidth={5} />
        {shapes.map(({ zone, points }) => (
          <G key={zone.id} onPress={() => onZonePress(zone)}>
            <SvgPolygon points={points} fill={`${getStatusColor(zone.status)}55`} stroke={getStatusColor(zone.status)} strokeWidth={3} />
            <Circle cx={project(zone.centerLat, zone.centerLon).x} cy={project(zone.centerLat, zone.centerLon).y} r={5} fill={getStatusColor(zone.status)} />
          </G>
        ))}
        {snapshot.stations.map((station) => {
          const point = project(station.latitude, station.longitude);
          return <Circle key={station.id} cx={point.x} cy={point.y} r={3.2} fill={colors.text} stroke={colors.cardSolid} strokeWidth={1.5} />;
        })}
      </Svg>
      <View style={[styles.legend, { backgroundColor: colors.cardSolid }]}>
        <Text style={[styles.legendText, { color: colors.textSecondary }]}>● станции · цвет — статус зоны</Text>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, overflow: "hidden" },
  legend: { position: "absolute", left: 10, bottom: 10, paddingHorizontal: 9, paddingVertical: 6, borderRadius: 12 },
  legendText: { fontSize: 10, fontWeight: "600" },
});

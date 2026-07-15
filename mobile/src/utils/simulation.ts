type RadiationStatus = "normal" | "elevated" | "dangerous" | "critical";

export const RADIATION_THRESHOLDS = { normal: 0.3, elevated: 0.6, dangerous: 1.2 } as const;

export function statusForLevel(level: number): RadiationStatus {
  if (level < RADIATION_THRESHOLDS.normal) return "normal";
  if (level < RADIATION_THRESHOLDS.elevated) return "elevated";
  if (level < RADIATION_THRESHOLDS.dangerous) return "dangerous";
  return "critical";
}

export function clampSafeLevel(current: number, delta: number): number {
  return Math.round(Math.max(0.05, Math.min(0.56, current + delta)) * 1000) / 1000;
}

export function isFutureIso(value: string | undefined, now: number): boolean {
  return !!value && new Date(value).getTime() > now;
}

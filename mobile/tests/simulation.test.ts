import assert from "node:assert/strict";
import test from "node:test";

import { clampSafeLevel, isFutureIso, statusForLevel } from "../src/utils/simulation.ts";

test("radiation thresholds use one shared scale", () => {
  assert.equal(statusForLevel(0.299), "normal");
  assert.equal(statusForLevel(0.3), "elevated");
  assert.equal(statusForLevel(0.6), "dangerous");
  assert.equal(statusForLevel(1.2), "critical");
});

test("safe random walk cannot become critical", () => {
  assert.equal(clampSafeLevel(0.55, 2), 0.56);
  assert.equal(statusForLevel(clampSafeLevel(0.55, 2)), "elevated");
  assert.equal(clampSafeLevel(0.06, -2), 0.05);
});

test("persisted alert expiry is evaluated against wall clock", () => {
  const now = Date.now();
  assert.equal(isFutureIso(new Date(now + 1_000).toISOString(), now), true);
  assert.equal(isFutureIso(new Date(now - 1).toISOString(), now), false);
  assert.equal(isFutureIso(undefined, now), false);
});

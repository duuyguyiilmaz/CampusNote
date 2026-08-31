import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    // The emulator is a single shared server, so parallel files would write over
    // each other's documents. One worker keeps the seeding in setup() honest.
    fileParallelism: false,
    testTimeout: 20000,
    hookTimeout: 20000,
  },
});

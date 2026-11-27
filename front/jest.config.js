/** @type {import('jest').Config} */
module.exports = {
  preset: 'jest-preset-angular',
  setupFilesAfterEnv: ['<rootDir>/setup-jest.ts'],
  testPathIgnorePatterns: ['<rootDir>/node_modules/', '<rootDir>/dist/', '<rootDir>/cypress/'],
  collectCoverageFrom: [
    '<rootDir>/src/app/**/*.ts',
    '!<rootDir>/src/app/**/*.spec.ts',
    '!<rootDir>/src/app/core/models/**',
    '!<rootDir>/src/app/app.config.ts',
    '!<rootDir>/src/app/app.routes.ts',
  ],
  coverageDirectory: '<rootDir>/coverage/jest',
  coverageReporters: ['text-summary', 'html', 'lcov'],
};

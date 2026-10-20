// Karma's Windows absolute URLs do not serve dynamically imported chunks reliably.
config.webpack.module.parser = {
    javascript: { dynamicImportMode: 'eager' }
};
config.failOnEmptyTestSuite = true;

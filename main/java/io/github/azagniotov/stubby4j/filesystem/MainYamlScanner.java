package io.github.azagniotov.stubby4j.filesystem;

import io.github.azagniotov.stubby4j.cli.ANSITerminal;
import io.github.azagniotov.stubby4j.stubs.StubRepository;
import io.github.azagniotov.stubby4j.yaml.YAMLParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;

import static io.github.azagniotov.stubby4j.utils.FileUtils.BR;

public final class MainYamlScanner implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainYamlScanner.class);

    private final long sleepTime;
    private final StubRepository stubRepository;

    public MainYamlScanner(final StubRepository stubRepository, final long sleepTime) {
        this.sleepTime = sleepTime;
        this.stubRepository = stubRepository;
        ANSITerminal.status(String.format("Main YAML scan enabled, watching %s", stubRepository.getYAMLConfigCanonicalPath()));
        LOGGER.debug("Main YAML scan enabled, watching {}.", stubRepository.getYAMLConfigCanonicalPath());
    }

    @Override
    public void run() {

        try {
            final File dataYaml = stubRepository.getYAMLConfig();
            long mainYamlLastModified = dataYaml.lastModified();

            while (!Thread.currentThread().isInterrupted()) {

                Thread.sleep(sleepTime);

                final long currentFileModified = dataYaml.lastModified();
                if (mainYamlLastModified >= currentFileModified) {
                    continue;
                }

                ANSITerminal.info(String.format("%sMain YAML scan detected change in %s%s", BR, stubRepository.getYAMLConfigCanonicalPath(), BR));
                LOGGER.info("Main YAML scan detected change in  {}.", stubRepository.getYAMLConfigCanonicalPath());

                try {
                    mainYamlLastModified = currentFileModified;
                    stubRepository.refreshStubsFromYAMLConfig(new YAMLParser());

                    ANSITerminal.ok(String.format("%sSuccessfully performed live refresh of main YAML file from: %s on [" + new Date().toString().trim() + "]%s",
                            BR, dataYaml.getAbsolutePath(), BR));
                    LOGGER.info("Successfully performed live refresh of main YAML from: {}.",
                            dataYaml.getAbsolutePath());
                } catch (final Exception ex) {
                    ANSITerminal.error("Could not refresh main YAML configuration, in-memory stubs remain untouched." + ex.toString());
                    LOGGER.error("Could not refresh main YAML configuration, in-memory stubs remain untouched.", ex);
                }
            }

        } catch (final Exception ex) {
            LOGGER.error("Could not perform live YAML scan.", ex);
        }
    }
}
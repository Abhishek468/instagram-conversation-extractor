package com.instagram.extractor.export;

import com.instagram.extractor.model.Attachment;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MediaResolverTest {

    // @Test
    void resolvesExistingMediaFile() throws Exception {

        Path exportRoot =
                Path.of(
                        "src",
                        "test",
                        "resources",
                        "export"
                ).toAbsolutePath();

        Path relativePath =
                Path.of(
                        "your_instagram_activity",
                        "messages",
                        "inbox",
                        "myra_1796143968442156",
                        "photos",
                        "1327012712753719.jpg"
                );

        Path actualFile =
                exportRoot.resolve(relativePath);

        if (!Files.exists(actualFile)) {
            System.out.println(
                    "Skipping filesystem assertion because "
                            + "the referenced media file is not "
                            + "present in the test export."
            );
            return;
        }

        Attachment attachment =
                new Attachment(
                        Attachment.Type.PHOTO,
                        relativePath.toString(),
                        null
                );

        MediaResolver resolver =
                new MediaResolver(exportRoot);

        Path resolved =
                resolver.resolve(attachment);

        assertEquals(
                actualFile.normalize(),
                resolved
        );

        assertTrue(
                resolver.exists(attachment)
        );

        System.out.println();
        System.out.println(
                "========================================"
        );
        System.out.println(
                "MEDIA RESOLVER TEST"
        );
        System.out.println(
                "========================================"
        );
        System.out.println(
                "Type: "
                        + attachment.type()
        );
        System.out.println(
                "Resolved: "
                        + resolved
        );
        System.out.println(
                "Exists: "
                        + resolver.exists(attachment)
        );
        System.out.println(
                "========================================"
        );
    }

    @Test
    void rejectsPathOutsideExportRoot() {

        Path exportRoot =
                Path.of(
                        "src",
                        "test",
                        "resources",
                        "export"
                );

        Attachment attachment =
                new Attachment(
                        Attachment.Type.PHOTO,
                        "../../outside/file.jpg",
                        null
                );

        MediaResolver resolver =
                new MediaResolver(exportRoot);

        assertThrows(
                SecurityException.class,
                () -> resolver.resolve(attachment)
        );
    }
}
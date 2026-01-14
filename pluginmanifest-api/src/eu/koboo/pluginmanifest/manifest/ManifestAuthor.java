package eu.koboo.pluginmanifest.manifest;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class ManifestAuthor {

    String name;
    String email;
    String url;
}

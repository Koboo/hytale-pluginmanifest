package eu.koboo.pluginmanifest.api;

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

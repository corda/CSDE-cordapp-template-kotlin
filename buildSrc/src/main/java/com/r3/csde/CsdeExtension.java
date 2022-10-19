
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;


// Version A
/*
public class CsdeExtension {
    CsdeExtension(
            ObjectFactory objects,
            ProviderFactory provider
    )
    {
        Property<String> serverUrl = objects.property(String.class);
        Property<String> dbContainerName = objects.property(String.class);
        Property<ConfigurableFileCollection> files = objects.property(ConfigurableFileCollection.class);
    }


}

 */

// Version B
public class CsdeExtension {
    private String serverUrl;
    private String dbContainerName;
}



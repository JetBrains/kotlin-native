#Publishing to maven.

Publishing the kotlin native artifacts borrows mechanisms introduced in Gradle Native support, e.g. gradle metadata feature,
thus there are some additional steps are required. First of all gradle version it shouldn't be less then gradle version of 
kotlin native plugin dependens on (currently gradle-4.7). before gradle-5.0 feature GRADLE_METADATA should be enabled for build.
e.g. in settings.gradle
````
enableFeaturePreview('GRADLE_METADATA')
````

Some maven repositories requires some declarations in `pom` files, that should be presents in all auxilary `pom` files (
platform x build types). To meet this requirement kotlin native plugin offers special syntax:

 ````
 konanArtifacts {
     interop('libcurl') {
         target('linux') {
             includeDirs.headerFilterOnly '/usr/include'
         }
         target('macbook') {
             includeDirs.headerFilterOnly '/opt/local/include', '/usr/local/include'
         }
         pom {
             withXml {
                 def root = asNode()
                 root.appendNode('name', 'libcurl interop library')
                 root.appendNode('description', 'A library providing interoperability with host libcurl')
             }
         }
     }
 }

 ````
 In this example `name` and `description` tags will be added to each `pom` file generated for _libcurl_ published artifact.
// Export JSON, import JSON
import * as THREE from 'three';
import { createButton } from "./elements.js";
import { getUrl } from "./conversions.js"
import { setAnnotationLabel } from "./sparql.js";

/**
 * Save annotations
 */
export function save(scene) {

  createButton({
    id: "save",
    innerHtml: "<i class=\"fas fa-save\"></i>",
    title: "Save"
  }).addEventListener("click", function () {
    const annotationsDiv = document.getElementById("annotations-div");

    if (annotationsDiv) {
      const checkboxes = annotationsDiv.querySelectorAll('input[type="checkbox"]:checked');

      if (checkboxes.length === 1) {
        // Single checkbox selection = save to the same file
        const selectedUrl = checkboxes[0].value;
        serializeScene(scene, selectedUrl);
      } else {
        // No checkboxes selected or multiple selected = save to new file
        const label = prompt("Enter a label for this annotation set:", "My Annotation Set");
        serializeScene(scene, null, label);
      }
    } else {
      serializeScene(scene); // Save to a new file
    }
  });

  async function serializeScene(scene, postUrl = null, label = null) {
    let serializedObjects = [];
    let processedObjects = new Set(); // To track processed objects

    function serializeObjectWithChildren(obj) {
      let serializedObj = obj.toJSON();
      // Mark all children as processed to avoid double serialization
      obj.traverse(child => {
        if (child.name.includes("annotation")) {
          processedObjects.add(child.id); // Use unique object ID for tracking
        }
      });
      return serializedObj;
    }

    scene.traverse(obj => {
      // Skip if this object has already been processed
      if (processedObjects.has(obj.id)) return;

      if (obj.type === 'Group') {
        let hasRelevantChildren = obj.children.some(child => child.name.includes("annotation"));
        if (hasRelevantChildren) {
          // Serialize the group and mark its relevant children as processed
          serializedObjects.push(serializeObjectWithChildren(obj));
        }
      } else if (obj.name.includes("annotation")) {
        // Serialize individual objects not yet processed
        serializedObjects.push(serializeObjectWithChildren(obj));
      }
    });

    // Add object with "image" and "type" for LDP
    const url = getUrl(scene);
    const parts = url.split("?iiif=");
    let myObject = {
      image: parts[1],
      type: "hal:Annotation"
    };
    serializedObjects.push(myObject);

    // Determine the URL for POST
    if (!postUrl) {
      let sections = parts[1].split("/");
      sections.pop(); // remove the svs
      postUrl = `${sections.join("/")}/${crypto.randomUUID()}.json`; // add uuid
    }

    // First save the serialized objects
    try {
      const response = await fetch(postUrl, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(serializedObjects)
      });

      if (response.ok) {
        console.log('File created successfully.', response);
      } else {
        console.error('Error creating file:', response.status, response.statusText);
        return;  // Stop execution if the file creation fails
      }
    } catch (error) {
      console.error('Fetch error:', error);
      return;  // Stop execution if there is a fetch error
    }

    if (label) {
      // After the resource is created, set the annotation label
      try {
        await setAnnotationLabel(postUrl, label);
      } catch (error) {
        console.error('Error setting annotation label:', error);
      }
    }

    console.log(serializedObjects);
    alert('Annotations saved successfully.');
  }
}

export function deserializeScene(scene, serializedObjects) {
  const loader = new THREE.ObjectLoader();
  const objects = [];

  serializedObjects.forEach(serializedData => {
    if (typeof serializedData === 'string') {
      serializedData = JSON.parse(serializedData);
    }

    // Check if the object should be deserialized
    if (Object.keys(serializedData).length === 2 &&
      serializedData.hasOwnProperty('image') &&
      serializedData.hasOwnProperty('type')) {
      // Skip this object as it only contains "image" and "type"
      // console.log('Skipping object with only image and type fields:', serializedData);
      return;
    }

    // Deserialize the object
    const object = loader.parse(serializedData);

    // Add the deserialized object to the scene and objects array
    scene.add(object);
    objects.push(object);
  });

  return objects;
}

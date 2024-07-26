// Export JSON, import JSON
import * as THREE from 'three';
import { createButton } from "./elements.js";
import { getUrl } from "./conversions.js"

/**
 * Save annotations
 */
export function save(scene) {
  const demo = false;

  createButton({
    id: "save",
    innerHtml: "<i class=\"fas fa-save\"></i>",
    title: "Save"
  }).addEventListener("click", function () {
    serializeScene(scene);
  });

  let serializedObjects = [];

  function serializeScene(scene) {
    serializedObjects = [];
    let processedObjects = new Set(); // To track processed objects

    function serializeObjectWithChildren(obj) {
      let serializedObj = obj.toJSON();
      // Mark all children as processed to avoid double serialization
      obj.traverse((child) => {
        if (child.name.includes("annotation")) {
          processedObjects.add(child.id); // Use unique object ID for tracking
        }
      });
      return serializedObj;
    }

    scene.traverse((obj) => {
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

    // Add object with properties
    const url = getUrl(scene);
    const parts = url.split("?iiif=");
    let myObject = {
      image: parts[1],
      type: "hal:Annotation"
    }
    serializedObjects.push(myObject);

    // Save serializedObjects to database
    let sections = parts[1].split("/");
    sections.pop();
    let postUrl = `${sections.join("/")}/${crypto.randomUUID()}.json`;

    const postJSONData = async () => {
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
        }
      } catch (error) {
        console.error('Fetch error:', error);
      }
    };

    postJSONData();

    console.log(serializedObjects);
    // console.log(JSON.stringify(serializedObjects));
    alert('Scene serialized successfully.');
  }

  //*******************************
  // For demonstration and testing:
  if (demo) {
    createButton({
      id: "clear",
      innerHtml: "<i class=\"fas fa-skull\"></i>",
      title: "clear"
    }).addEventListener("click", function () {
      let objectsToRemove = [];

      function findAnnotations(obj) {
        if (obj.name.includes("annotation")) {
          // Add the object to the removal list
          objectsToRemove.push(obj);
        } else if (obj.children && obj.children.length) {
          // If the object has children, check them too
          obj.children.forEach(findAnnotations);
        }
      }

      // Start the search with the top-level children of the scene
      scene.children.forEach(findAnnotations);

      // Now remove the collected objects and dispose of their resources
      objectsToRemove.forEach(obj => {
        if (obj.parent) {
          obj.parent.remove(obj); // Ensure the object is removed from its parent
        } else {
          scene.remove(obj); // Fallback in case the object is directly a child of the scene
        }
        if (obj.geometry) obj.geometry.dispose();
        if (obj.material) {
          // In case of an array of materials
          if (Array.isArray(obj.material)) {
            obj.material.forEach(material => material.dispose());
          } else {
            obj.material.dispose();
          }
        }
      });
    });

    createButton({
      id: "deserialize",
      innerHtml: "<i class=\"fa-solid fa-image\"></i>",
      title: "deserialize"
    }).addEventListener("click", function () {
      deserializeScene(scene, serializedObjects);
    });
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

import { createButton } from "./elements.js";
import { getUrl } from "./conversions.js";
import { deserializeScene } from "./save.js";

export function fetchAnnotations(scene) {
  const button = createButton({
    id: "annotations-button",
    innerHtml: "<i class=\"fas fa-comment-alt\"></i>",
    title: "Fetch Annotations"
  });

  let objectMap = new Map();

  button.addEventListener('click', () => {
    if (document.getElementById("annotations-div")) {
      document.getElementById("annotations-div").remove();
      // document.getElementById("annotations-div").style.display = "none";
    } else {
      const div = document.createElement('div');
      div.id = "annotations-div";
      document.body.appendChild(div);

      const url = getUrl(scene);
      const parts = url.split("?iiif=");
      const annotationUrl = parts[1];

      fetchA(annotationUrl).then(annotationArray => {
        if (annotationArray && annotationArray.length > 0) {
          displayPopup(div, annotationArray);
        }
      });
    }
  });

  // Function to fetch data and return the annotation array
  async function fetchA(url) {
    try {
      const response = await fetch(url, {
        method: 'GET',
        headers: {
          'Accept': 'application/ld+json',
          'Prefer': 'return=representation; shacl="https://halcyon.is/ns/AnnotationsShape"; include=https://halcyon.is/ns/annotation'
        }
      });

      // Check if the response is OK and has content
      if (!response.ok) {
        console.error(`HTTP error! status: ${response.status}`);
        alert(`HTTP error! status: ${response.status}`);
        return [];
      }

      const responseText = await response.text();
      // console.log('Raw response text:', responseText);

      // If response text is empty, alert the user and print the URL
      if (!responseText) {
        console.error('Response text is empty. URL:', url);
        alert(`Error: Response text is empty. URL: ${url}`);
        return [];
      }

      // Try parsing the JSON
      let data;
      try {
        data = JSON.parse(responseText);
      } catch (e) {
        console.error('Error parsing JSON:', e);
        alert('Error parsing JSON');
        return [];
      }

      if (!data.annotation) {
        // console.log('No annotations:', JSON.stringify(data));
        alert('No annotations yet. Please create, then save.');
        return [];
      }

      return data.annotation;
    } catch (error) {
      console.error('Error:', error);
      alert('Error fetching annotations');
      return [];
    }
  }

  function displayPopup(div, annotationArray) {
    // Create and style the close button
    const closeButton = document.createElement('span');
    closeButton.innerHTML = '&times;';
    closeButton.style.position = 'absolute';
    closeButton.style.top = '10px';
    closeButton.style.right = '10px';
    closeButton.style.cursor = 'pointer';
    closeButton.style.fontSize = '20px';

    // Add click event to hide the div
    closeButton.addEventListener('click', () => {
      div.remove();
      // div.style.display = 'none';
    });

    // Add the close button to the div
    div.appendChild(closeButton);

    // Add checkboxes based on the annotation array
    annotationArray.forEach(annotation => {
      const label = document.createElement('label');
      const checkbox = document.createElement('input');
      checkbox.type = 'checkbox';
      checkbox.value = annotation;

      label.appendChild(checkbox);
      // label.appendChild(document.createTextNode(`Annotation ${index + 1}`));
      let sections = annotation.split("/");
      label.appendChild(document.createTextNode(sections[sections.length - 1]));
      div.appendChild(label);
      div.appendChild(document.createElement('br'));

      checkbox.addEventListener('change', function () {
        if (this.checked) {
          if (!objectMap.has(annotation)) {
            fetch(annotation)
              .then(response => {
                if (!response.ok) {
                  throw new Error('URL does not exist');
                }
                return response.json();
              })
              .then(data => {
                try {
                  const objects = deserializeScene(scene, data);
                  objectMap.set(annotation, objects);
                } catch (e) {
                  console.error('Deserialization error:', e);
                }
              })
              .catch(error => alert(error.message));
          } else {
            objectMap.get(annotation).forEach(obj => {
              obj.visible = true;
            });
          }
        } else {
          if (objectMap.has(annotation)) {
            objectMap.get(annotation).forEach(obj => {
              obj.visible = false;
              // console.log(`Hiding object: ${obj.name}`);
            });
          }
        }
      });
    });

    // Style the div
    div.style.position = 'fixed';
    div.style.top = '30px';
    div.style.left = '30px';
    div.style.width = '150px';
    div.style.height = 'auto';
    div.style.padding = '10px';
    div.style.border = '1px solid #ccc';
    div.style.background = '#fff';
    div.style.boxShadow = '0 0 10px rgba(0, 0, 0, 0.1)';
    div.style.zIndex = '1000';

    // Make the popup draggable
    dragElement(div);
  }

  function dragElement(element) {
    let pos1 = 0, pos2 = 0, pos3 = 0, pos4 = 0;
    element.onmousedown = dragMouseDown;

    function dragMouseDown(e) {
      e.preventDefault();
      pos3 = e.clientX;
      pos4 = e.clientY;
      document.onmouseup = closeDragElement;
      document.onmousemove = elementDrag;
    }

    function elementDrag(e) {
      e.preventDefault();
      pos1 = pos3 - e.clientX;
      pos2 = pos4 - e.clientY;
      pos3 = e.clientX;
      pos4 = e.clientY;
      element.style.top = (element.offsetTop - pos2) + "px";
      element.style.left = (element.offsetLeft - pos1) + "px";
    }

    function closeDragElement() {
      document.onmouseup = null;
      document.onmousemove = null;
    }
  }
}

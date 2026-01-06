const statusElement = document.getElementById("packStatus");
const errorElement = document.getElementById("error");
const contentElement = document.getElementById("content");
const listElement = document.getElementById("competencyList");

function showError(message) {
  errorElement.textContent = message;
  errorElement.classList.remove("hidden");
  contentElement.classList.add("hidden");
}

function formatDate(value) {
  const date = new Date(value);
  return date.toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
}

function statusClass(status) {
  if (status === "ExpiringSoon") return "chip expiring";
  if (status === "Expired") return "chip expired";
  return "chip";
}

function renderPack(data, token) {
  statusElement.textContent = `Expires ${formatDate(data.expiresAt)}`;
  listElement.innerHTML = "";

  data.competencies.forEach((competency) => {
    const card = document.createElement("article");
    card.className = "card";

    const meta = document.createElement("div");
    meta.className = "meta";

    const expiryChip = document.createElement("span");
    expiryChip.className = statusClass(competency.status);
    expiryChip.textContent = competency.status;

    const dateChip = document.createElement("span");
    dateChip.className = "chip";
    dateChip.textContent = `Expiry ${formatDate(competency.expiresAt)}`;

    meta.append(expiryChip, dateChip);

    const evidenceList = document.createElement("ul");
    evidenceList.className = "evidence";

    if (competency.evidence.length === 0) {
      const item = document.createElement("li");
      item.textContent = "No evidence attached.";
      evidenceList.appendChild(item);
    } else {
      competency.evidence.forEach((evidence) => {
        const item = document.createElement("li");
        const link = document.createElement("a");
        link.href = `${window.SHARE_API_BASE}/share/${token}/download/${evidence.id}`;
        link.textContent = `${evidence.fileName} (${Math.round(evidence.size / 1024)} KB)`;
        link.target = "_blank";
        item.appendChild(link);
        evidenceList.appendChild(item);
      });
    }

    const description = competency.description ? `<p>${competency.description}</p>` : "";

    card.innerHTML = `
      <h2>${competency.title}</h2>
      ${description}
    `;

    card.appendChild(meta);
    card.appendChild(evidenceList);
    listElement.appendChild(card);
  });

  contentElement.classList.remove("hidden");
}

async function init() {
  const params = new URLSearchParams(window.location.search);
  const token = params.get("token");

  if (!token) {
    showError("Missing share token. Ensure the link includes ?token=...");
    statusElement.textContent = "Invalid link";
    return;
  }

  if (!window.SHARE_API_BASE || window.SHARE_API_BASE.includes("YOUR_API_URL")) {
    showError("Share viewer is not configured. Update config.js with the API URL.");
    statusElement.textContent = "Configuration required";
    return;
  }

  try {
    const response = await fetch(`${window.SHARE_API_BASE}/share/${token}`);
    if (!response.ok) {
      throw new Error("Share pack not found or expired.");
    }

    const data = await response.json();
    renderPack(data, token);
  } catch (error) {
    showError(error.message || "Unable to load share pack.");
    statusElement.textContent = "Unavailable";
  }
}

init();

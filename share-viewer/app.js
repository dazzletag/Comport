const statusElement = document.getElementById("packStatus");
const errorElement = document.getElementById("error");
const contentElement = document.getElementById("content");
const listElement = document.getElementById("competencyList");
const nurseNameElement = document.getElementById("nurseName");
const registrationTypeElement = document.getElementById("registrationType");
const emailAddressElement = document.getElementById("emailAddress");
const nmcPinElement = document.getElementById("nmcPin");
const printButton = document.getElementById("printBtn");

function showError(message) {
  errorElement.textContent = message;
  errorElement.classList.remove("hidden");
  contentElement.classList.add("hidden");
}

function formatDate(value) {
  const date = new Date(value);
  return date.toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
}

function expiryText(expiresAt) {
  const now = new Date();
  const expiry = new Date(expiresAt);
  const diffDays = Math.ceil((expiry - now) / (1000 * 60 * 60 * 24));
  if (diffDays < 0) return `Expired ${Math.abs(diffDays)} days ago`;
  if (diffDays === 0) return "Expires today";
  if (diffDays === 1) return "Expires in 1 day";
  return `Expires in ${diffDays} days`;
}

function statusClass(status) {
  if (status === "ExpiringSoon") return "chip expiring";
  if (status === "Expired") return "chip expired";
  return "chip";
}

function statusLabel(status) {
  if (status === "ExpiringSoon") return "Expiring soon";
  if (status === "Expired") return "Expired";
  return "Valid";
}

function renderPack(data, token) {
  statusElement.textContent = `Pack expiry ${formatDate(data.expiresAt)}`;
  nurseNameElement.textContent = data.nurseName || "Not provided";
  registrationTypeElement.textContent = data.registrationType || "Not provided";
  emailAddressElement.textContent = data.email || "Not provided";
  nmcPinElement.textContent = data.nmcPin || "Not shared";
  listElement.innerHTML = "";

  data.competencies.forEach((competency) => {
    const card = document.createElement("article");
    card.className = "card";

    const meta = document.createElement("div");
    meta.className = "meta";

    const expiryChip = document.createElement("span");
    expiryChip.className = statusClass(competency.status);
    expiryChip.textContent = statusLabel(competency.status);

    const dateChip = document.createElement("span");
    dateChip.className = "chip";
    dateChip.textContent = expiryText(competency.expiresAt);

    const categoryChip = document.createElement("span");
    categoryChip.className = "chip subtle";
    categoryChip.textContent = competency.category || "Mandatory";

    meta.append(expiryChip, dateChip, categoryChip);

    const evidenceList = document.createElement("ul");
    evidenceList.className = "evidence";

    if (competency.evidence.length === 0) {
      const item = document.createElement("li");
      item.textContent = "No evidence attached.";
      evidenceList.appendChild(item);
    } else {
      competency.evidence.forEach((evidence) => {
        const item = document.createElement("li");
        const type = document.createElement("span");
        type.className = "evidence__type";
        type.textContent = evidence.contentType?.startsWith("image/") ? "Photo" : "Document";
        const link = document.createElement("a");
        link.href = `${window.SHARE_API_BASE}/share/${token}/download/${evidence.id}`;
        link.textContent = `${evidence.fileName} (${Math.round(evidence.size / 1024)} KB)`;
        link.target = "_blank";
        item.append(type, link);
        if (evidence.note) {
          const note = document.createElement("div");
          note.className = "evidence__note";
          note.textContent = evidence.note;
          item.appendChild(note);
        }
        evidenceList.appendChild(item);
      });
    }

    const description = competency.description ? `<p>${competency.description}</p>` : "";
    const timeline = `
      <div class="timeline">
        <div><span>Achieved</span>${formatDate(competency.achievedAt)}</div>
        <div><span>Expiry</span>${formatDate(competency.expiresAt)}</div>
      </div>
    `;

    card.innerHTML = `
      <h2>${competency.title}</h2>
      ${description}
      ${timeline}
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

printButton?.addEventListener("click", () => {
  window.print();
});

init();

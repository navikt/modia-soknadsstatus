const templateEntries = Array.from(document.querySelectorAll('.templates > *'))
    .map(it => [it.dataset.name, it.innerHTML.trim()]);
const templates = Object.fromEntries(templateEntries);
const main = document.querySelector('main');

function createHtml(html, variables) {
    const node = document.createElement('div');
    node.innerHTML = html.replace(/{{(.+?)}}/g, (_, group) => variables[group]);
    return node.firstChild
}

async function fetchJson(input, init) {
    const response = await fetch(input, init)
    if (response.ok) {
        return response.json()
    } else {
        throw new Error(response);
    }
}

async function runapp() {
    setupEventListener();
    await setupPostQueues();
    await setupKafkalog();
}

function setupEventListener() {
    main.addEventListener('submit', (e) => {
        e.preventDefault();
        postFormData(e.target);
    });

    main.addEventListener('animationend', (e) => {
        e.target.remove();
    });
}

async function setupPostQueues() {
    const kildeTemplate = templates['kilde'];

    const kilder = await fetchJson('/api/kilder');

    for (const kilde of kilder) {
        main.appendChild(createHtml(kildeTemplate, kilde));
    }
}
async function postFormData(form) {
    const formdata = new FormData(form);
    const data = Object.fromEntries(formdata.entries());
    const statusEl = document.createElement('span');
    statusEl.classList.add('status');
    statusEl.textContent = 'Sending...';
    form.appendChild(statusEl);

    const response = await fetch(`/api/kilder/${data.resourceId}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data.content)
    });

    if (response.ok) {
        statusEl.classList.add('fade-out');
        statusEl.textContent = 'Sent...';
    } else {
        statusEl.classList.add('error');
        statusEl.textContent = 'Error';
    }
}

async function setupKafkalog() {
    const kafkalogTemplate = templates['kafkalog'];
    main.appendChild(createHtml(kafkalogTemplate));

    const kafkalog = main.querySelector('.kafkalog textarea');
    const ws = new WebSocket('ws://localhost:9999/api/ws');
    ws.addEventListener('message', (e) => {
        kafkalog.value = `${kafkalog.value}\n${e.data}`;
    });
}


runapp();
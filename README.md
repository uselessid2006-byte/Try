# CloudStream Custom Repository (DesiTales 2)

## Setup Instructions

1. **Create a new GitHub repository** (e.g., `cloudstream-repo`).
2. **Edit `repo.json`**: Replace `YOUR_USERNAME` and `YOUR_REPO` with your GitHub username and repository name.
3. **Upload all files** from this archive into the root of your repository (including the `.github` folder).
4. **Enable Workflow Permissions**:
   - Open your GitHub repository **Settings -> Actions -> General**.
   - Under **Workflow permissions**, select **Read and write permissions** and click **Save**.
5. **Run the Build**:
   - Pushing to `main` (or clicking **Run workflow** in the **Actions** tab) automatically builds `DesiTales2.cs3` and publishes it to the `builds` branch.
6. **Install in CloudStream**:
   - Open CloudStream -> **Settings -> Extensions -> Add repository**.
   - Paste: `https://raw.githubusercontent.com/YOUR_USERNAME/YOUR_REPO/main/repo.json`
   - Download **DesiTales 2** and enable **Adult / NSFW** content under **Settings -> General**.

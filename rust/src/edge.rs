// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

use crate::config::ManagerConfig;
use reqwest::Client;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::error::Error;
use std::path::PathBuf;

use crate::config::ARCH::{ARM64, X32};
use crate::config::OS::{LINUX, MACOS, WINDOWS};
use crate::downloads::{parse_json_from_url, read_version_from_link};
use crate::files::{compose_driver_path_in_cache, BrowserPath};
use crate::metadata::{
    create_driver_metadata, get_driver_version_from_metadata, get_metadata, write_metadata,
};
use crate::{
    create_browser_metadata, create_http_client, download_to_tmp_folder,
    get_browser_version_from_metadata, path_buf_to_string, uncompress, Logger, SeleniumManager,
    BETA, DASH_DASH_VERSION, DEV, NIGHTLY, OFFLINE_REQUEST_ERR_MSG, REG_VERSION_ARG, STABLE,
};

pub const EDGE_NAMES: &[&str] = &["edge", "msedge", "microsoftedge"];
pub const EDGEDRIVER_NAME: &str = "msedgedriver";
const DRIVER_URL: &str = "https://msedgedriver.azureedge.net/";
const LATEST_STABLE: &str = "LATEST_STABLE";
const LATEST_RELEASE: &str = "LATEST_RELEASE";
const BROWSER_URL: &str = "https://edgeupdates.microsoft.com/api/products";
const EDGE_MACOS_APP_NAME: &str = "Microsoft Edge.app/Contents/MacOS/Microsoft Edge";
const EDGE_BETA_MACOS_APP_NAME: &str = "Microsoft Edge Beta.app/Contents/MacOS/Microsoft Edge Beta";
const EDGE_DEV_MACOS_APP_NAME: &str = "Microsoft Edge Dev.app/Contents/MacOS/Microsoft Edge Dev";
const EDGE_CANARY_MACOS_APP_NAME: &str =
    "Microsoft Edge Canary.app/Contents/MacOS/Microsoft Edge Canary";

pub struct EdgeManager {
    pub browser_name: &'static str,
    pub driver_name: &'static str,
    pub config: ManagerConfig,
    pub http_client: Client,
    pub log: Logger,
    pub browser_url: Option<String>,
}

impl EdgeManager {
    pub fn new() -> Result<Box<Self>, Box<dyn Error>> {
        let browser_name = EDGE_NAMES[0];
        let driver_name = EDGEDRIVER_NAME;
        let config = ManagerConfig::default(browser_name, driver_name);
        let default_timeout = config.timeout.to_owned();
        let default_proxy = &config.proxy;
        Ok(Box::new(EdgeManager {
            browser_name,
            driver_name,
            http_client: create_http_client(default_timeout, default_proxy)?,
            config,
            log: Logger::new(),
            browser_url: None,
        }))
    }

    // TODO check
    fn get_browser_url(&self) -> Result<String, Box<dyn Error>> {
        let browser_url = self.browser_url.clone();
        Ok(browser_url.unwrap_or_default())
    }

    fn get_browser_binary_path_in_cache(&self) -> Result<PathBuf, Box<dyn Error>> {
        let browser_in_cache = self.get_browser_path_in_cache()?;
        if MACOS.is(self.get_os()) {
            let macos_app_name = if self.is_browser_version_beta() {
                EDGE_BETA_MACOS_APP_NAME
            } else if self.is_browser_version_dev() {
                EDGE_DEV_MACOS_APP_NAME
            } else if self.is_browser_version_nightly() {
                EDGE_CANARY_MACOS_APP_NAME
            } else {
                EDGE_MACOS_APP_NAME
            };
            Ok(browser_in_cache.join(macos_app_name))
        } else {
            Ok(browser_in_cache.join(self.get_browser_name_with_extension()))
        }
    }
}

impl SeleniumManager for EdgeManager {
    fn get_browser_name(&self) -> &str {
        self.browser_name
    }

    fn get_http_client(&self) -> &Client {
        &self.http_client
    }

    fn set_http_client(&mut self, http_client: Client) {
        self.http_client = http_client;
    }

    fn get_browser_path_map(&self) -> HashMap<BrowserPath, &str> {
        HashMap::from([
            (
                BrowserPath::new(WINDOWS, STABLE),
                r#"Microsoft\Edge\Application\msedge.exe"#,
            ),
            (
                BrowserPath::new(WINDOWS, BETA),
                r#"Microsoft\Edge Beta\Application\msedge.exe"#,
            ),
            (
                BrowserPath::new(WINDOWS, DEV),
                r#"Microsoft\Edge Dev\Application\msedge.exe"#,
            ),
            (
                BrowserPath::new(WINDOWS, NIGHTLY),
                r#"Microsoft\Edge SxS\Application\msedge.exe"#,
            ),
            (
                BrowserPath::new(MACOS, STABLE),
                r#"/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge"#,
            ),
            (
                BrowserPath::new(MACOS, BETA),
                r#"/Applications/Microsoft Edge Beta.app/Contents/MacOS/Microsoft Edge Beta"#,
            ),
            (
                BrowserPath::new(MACOS, DEV),
                r#"/Applications/Microsoft Edge Dev.app/Contents/MacOS/Microsoft Edge Dev"#,
            ),
            (
                BrowserPath::new(MACOS, NIGHTLY),
                r#"/Applications/Microsoft Edge Canary.app/Contents/MacOS/Microsoft Edge Canary"#,
            ),
            (BrowserPath::new(LINUX, STABLE), "/usr/bin/microsoft-edge"),
            (
                BrowserPath::new(LINUX, BETA),
                "/usr/bin/microsoft-edge-beta",
            ),
            (BrowserPath::new(LINUX, DEV), "/usr/bin/microsoft-edge-dev"),
        ])
    }

    fn discover_browser_version(&mut self) -> Result<Option<String>, Box<dyn Error>> {
        self.general_discover_browser_version(
            r#"HKCU\Software\Microsoft\Edge\BLBeacon"#,
            REG_VERSION_ARG,
            DASH_DASH_VERSION,
        )
    }

    fn get_driver_name(&self) -> &str {
        self.driver_name
    }

    fn request_driver_version(&mut self) -> Result<String, Box<dyn Error>> {
        let mut major_browser_version = self.get_major_browser_version();
        let cache_path = self.get_cache_path()?;
        let mut metadata = get_metadata(self.get_logger(), &cache_path);

        match get_driver_version_from_metadata(
            &metadata.drivers,
            self.driver_name,
            major_browser_version.as_str(),
        ) {
            Some(driver_version) => {
                self.log.trace(format!(
                    "Driver TTL is valid. Getting {} version from metadata",
                    &self.driver_name
                ));
                Ok(driver_version)
            }
            _ => {
                self.assert_online_or_err(OFFLINE_REQUEST_ERR_MSG)?;

                if self.is_browser_version_stable()
                    || major_browser_version.is_empty()
                    || self.is_browser_version_unstable()
                {
                    let latest_stable_url = format!("{}{}", DRIVER_URL, LATEST_STABLE);
                    self.log.debug(format!(
                        "Reading {} latest version from {}",
                        &self.driver_name, latest_stable_url
                    ));
                    let latest_driver_version = read_version_from_link(
                        self.get_http_client(),
                        latest_stable_url,
                        self.get_logger(),
                    )?;
                    major_browser_version =
                        self.get_major_version(latest_driver_version.as_str())?;
                    self.log.debug(format!(
                        "Latest {} major version is {}",
                        &self.driver_name, major_browser_version
                    ));
                }
                let driver_url = format!(
                    "{}{}_{}_{}",
                    DRIVER_URL,
                    LATEST_RELEASE,
                    major_browser_version,
                    self.get_os().to_uppercase()
                );
                self.log.debug(format!(
                    "Reading {} version from {}",
                    &self.driver_name, driver_url
                ));
                let driver_version =
                    read_version_from_link(self.get_http_client(), driver_url, self.get_logger())?;

                let driver_ttl = self.get_ttl();
                if driver_ttl > 0 && !major_browser_version.is_empty() {
                    metadata.drivers.push(create_driver_metadata(
                        major_browser_version.as_str(),
                        self.driver_name,
                        &driver_version,
                        driver_ttl,
                    ));
                    write_metadata(&metadata, self.get_logger(), cache_path);
                }

                Ok(driver_version)
            }
        }
    }

    fn request_browser_version(&mut self) -> Result<Option<String>, Box<dyn Error>> {
        Ok(None)
    }

    fn get_driver_url(&mut self) -> Result<String, Box<dyn Error>> {
        let driver_version = self.get_driver_version();
        let os = self.get_os();
        let arch = self.get_arch();
        let driver_label = if WINDOWS.is(os) {
            if ARM64.is(arch) {
                "arm64"
            } else if X32.is(arch) {
                "win32"
            } else {
                "win64"
            }
        } else if MACOS.is(os) {
            if ARM64.is(arch) {
                "mac64_m1"
            } else {
                "mac64"
            }
        } else {
            "linux64"
        };
        Ok(format!(
            "{}{}/edgedriver_{}.zip",
            DRIVER_URL, driver_version, driver_label
        ))
    }

    fn get_driver_path_in_cache(&self) -> Result<PathBuf, Box<dyn Error>> {
        Ok(compose_driver_path_in_cache(
            self.get_cache_path()?.unwrap_or_default(),
            self.driver_name,
            self.get_os(),
            self.get_platform_label(),
            self.get_driver_version(),
        ))
    }

    fn get_config(&self) -> &ManagerConfig {
        &self.config
    }

    fn get_config_mut(&mut self) -> &mut ManagerConfig {
        &mut self.config
    }

    fn set_config(&mut self, config: ManagerConfig) {
        self.config = config;
    }

    fn get_logger(&self) -> &Logger {
        &self.log
    }

    fn set_logger(&mut self, log: Logger) {
        self.log = log;
    }

    // TODO check
    fn download_browser(&mut self) -> Result<Option<PathBuf>, Box<dyn Error>> {
        let browser_version;
        let browser_name = self.browser_name;
        let mut metadata = get_metadata(self.get_logger(), self.get_cache_path()?);
        let major_browser_version = self.get_major_browser_version();

        // Browser version is checked in the local metadata
        match get_browser_version_from_metadata(
            &metadata.browsers,
            browser_name,
            &major_browser_version,
        ) {
            Some(version) => {
                self.get_logger().trace(format!(
                    "Browser with valid TTL. Getting {} version from metadata",
                    browser_name
                ));
                browser_version = version;
                self.set_browser_version(browser_version.clone());
            }
            _ => {
                // If not in metadata, discover version using Mozilla online metadata
                if self.is_browser_version_stable() || self.is_browser_version_empty() {
                    browser_version = self.request_latest_browser_version_from_online()?;
                } else {
                    browser_version = self.request_fixed_browser_version_from_online()?;
                }
                self.set_browser_version(browser_version.clone());

                let browser_ttl = self.get_ttl();
                if browser_ttl > 0
                    && !self.is_browser_version_empty()
                    && !self.is_browser_version_stable()
                {
                    metadata.browsers.push(create_browser_metadata(
                        browser_name,
                        &major_browser_version,
                        &browser_version,
                        browser_ttl,
                    ));
                    write_metadata(&metadata, self.get_logger(), self.get_cache_path()?);
                }
            }
        }
        self.get_logger().debug(format!(
            "Required browser: {} {}",
            browser_name, browser_version
        ));

        // Checking if browser version is in the cache
        let browser_binary_path = self.get_browser_binary_path_in_cache()?;
        if browser_binary_path.exists() {
            self.get_logger().debug(format!(
                "{} {} already in the cache",
                browser_name, browser_version
            ));
        } else {
            // If browser is not in the cache, download it
            let browser_url = self.get_browser_url()?;
            self.get_logger().debug(format!(
                "Downloading {} {} from {}",
                self.get_browser_name(),
                self.get_browser_version(),
                browser_url
            ));
            let (_tmp_folder, driver_zip_file) =
                download_to_tmp_folder(self.get_http_client(), browser_url, self.get_logger())?;

            let major_browser_version_int = self
                .get_major_browser_version()
                .parse::<i32>()
                .unwrap_or_default();
            uncompress(
                &driver_zip_file,
                &self.get_browser_path_in_cache()?,
                self.get_logger(),
                self.get_os(),
                None,
                None,
                Some(major_browser_version_int),
            )?;
        }
        if browser_binary_path.exists() {
            self.set_browser_path(path_buf_to_string(browser_binary_path.clone()));
            Ok(Some(browser_binary_path))
        } else {
            Ok(None)
        }
    }

    fn get_platform_label(&self) -> &str {
        let os = self.get_os();
        let arch = self.get_arch();
        if WINDOWS.is(os) {
            if ARM64.is(arch) {
                "win-arm64"
            } else if X32.is(arch) {
                "win32"
            } else {
                "win64"
            }
        } else if MACOS.is(os) {
            if ARM64.is(arch) {
                "mac-arm64"
            } else {
                "mac64"
            }
        } else {
            "linux64"
        }
    }

    // TODO check
    fn request_latest_browser_version_from_online(&mut self) -> Result<String, Box<dyn Error>> {
        let browser_version = self.get_browser_version();
        let edge_updates_url = if browser_version.is_empty() || self.is_browser_version_unstable() {
            BROWSER_URL.to_string()
        } else {
            format!("{}?view=enterprise", BROWSER_URL)
        };
        let edge_products = parse_json_from_url::<Vec<EdgeProduct>>(
            self.get_http_client(),
            edge_updates_url.clone(),
        )?;

        let edge_channel = if self.is_browser_version_beta() {
            "Beta"
        } else if self.is_browser_version_dev() {
            "Dev"
        } else if self.is_browser_version_nightly() {
            "Canary"
        } else {
            "Stable"
        };
        let products: Vec<&EdgeProduct> = edge_products
            .iter()
            .filter(|p| p.product.eq_ignore_ascii_case(edge_channel))
            .collect();
        self.get_logger().trace(format!("Products: {:?}", products));

        let os = self.get_os();
        let arch = self.get_arch();
        let arch_label = if WINDOWS.is(os) {
            if ARM64.is(arch) {
                "arm64"
            } else if X32.is(arch) {
                "x86"
            } else {
                "x64"
            }
        } else if MACOS.is(os) {
            "universal"
        } else {
            "x64"
        };
        let releases: Vec<&Release> = products
            .first()
            .unwrap()
            .releases
            .iter()
            .filter(|r| {
                r.platform.eq_ignore_ascii_case(os)
                    && r.architecture.eq_ignore_ascii_case(arch_label)
            })
            .collect();
        self.get_logger().trace(format!("Releases: {:?}", releases));

        let package_label = if WINDOWS.is(os) {
            "msi"
        } else if MACOS.is(os) {
            "pkg"
        } else {
            "deb"
        };
        let release = releases.first().unwrap();
        let artifacts: Vec<&Artifact> = release
            .artifacts
            .iter()
            .filter(|a| a.artifact_name.eq_ignore_ascii_case(package_label))
            .collect();
        self.get_logger()
            .trace(format!("Artifacts: {:?}", artifacts));

        let artifact = artifacts.first().unwrap();
        let browser_version = release.product_version.clone();
        self.browser_url = Some(artifact.location.clone());

        Ok(browser_version)
    }

    fn request_fixed_browser_version_from_online(&mut self) -> Result<String, Box<dyn Error>> {
        self.request_latest_browser_version_from_online()
    }
}

#[derive(Serialize, Deserialize, Debug)]
pub struct EdgeProduct {
    #[serde(rename = "Product")]
    pub product: String,
    #[serde(rename = "Releases")]
    pub releases: Vec<Release>,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct Release {
    #[serde(rename = "ReleaseId")]
    pub release_id: u32,
    #[serde(rename = "Platform")]
    pub platform: String,
    #[serde(rename = "Architecture")]
    pub architecture: String,
    #[serde(rename = "CVEs")]
    pub cves: Vec<String>,
    #[serde(rename = "ProductVersion")]
    pub product_version: String,
    #[serde(rename = "Artifacts")]
    pub artifacts: Vec<Artifact>,
    #[serde(rename = "PublishedTime")]
    pub published_time: String,
    #[serde(rename = "ExpectedExpiryDate")]
    pub expected_expiry_date: String,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct Artifact {
    #[serde(rename = "ArtifactName")]
    pub artifact_name: String,
    #[serde(rename = "Location")]
    pub location: String,
    #[serde(rename = "Hash")]
    pub hash: String,
    #[serde(rename = "HashAlgorithm")]
    pub hash_algorithm: String,
    #[serde(rename = "SizeInBytes")]
    pub size_in_bytes: u32,
}

import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @StateObject private var viewModel = NativeLoginViewModel()

    var body: some View {
        Group {
            if viewModel.shouldShowComposeApp {
                ComposeView()
                    .ignoresSafeArea()
            } else {
                NativeLoginView(viewModel: viewModel)
            }
        }
        .animation(.easeInOut(duration: 0.22), value: viewModel.shouldShowComposeApp)
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.willEnterForegroundNotification)) { _ in
            viewModel.refreshAuthState()
        }
    }
}

@MainActor
final class NativeLoginViewModel: ObservableObject {
    @Published var phone: String
    @Published var password: String = ""
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    @Published var shouldShowComposeApp: Bool

    private let bridge: NativeAuthBridge

    init(bridge: NativeAuthBridge = NativeAuthBridge()) {
        self.bridge = bridge
        self.phone = bridge.storedPhone()
        self.shouldShowComposeApp = bridge.hasValidToken()
    }

    var canLogin: Bool {
        !isLoading &&
        phone.trimmingCharacters(in: .whitespacesAndNewlines).count == 11 &&
        !password.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    func refreshAuthState() {
        if bridge.hasValidToken() {
            shouldShowComposeApp = true
        }
    }

    func continueWithComposeLogin() {
        shouldShowComposeApp = true
    }

    func login() {
        let normalizedPhone = phone.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedPassword = password.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !normalizedPhone.isEmpty, !normalizedPassword.isEmpty else {
            errorMessage = "请输入手机号和密码"
            return
        }

        guard normalizedPhone.range(of: #"^1\d{10}$"#, options: .regularExpression) != nil else {
            errorMessage = "请输入正确的手机号"
            return
        }

        errorMessage = nil
        isLoading = true

        bridge.loginWithPassword(phone: normalizedPhone, password: normalizedPassword) { [weak self] result in
            DispatchQueue.main.async {
                guard let self else { return }
                self.isLoading = false
                if result.success {
                    self.shouldShowComposeApp = true
                } else {
                    self.errorMessage = result.message
                }
            }
        }
    }
}

private struct NativeLoginView: View {
    @ObservedObject var viewModel: NativeLoginViewModel
    @FocusState private var focusedField: Field?

    private enum Field {
        case phone
        case password
    }

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 0.97, green: 0.98, blue: 1.0),
                    Color(red: 0.93, green: 0.95, blue: 0.99)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer(minLength: 56)

                    Image(systemName: "brain.head.profile")
                        .font(.system(size: 34, weight: .semibold))
                        .foregroundStyle(.black)
                        .frame(width: 58, height: 58)
                        .background(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                        .shadow(color: .black.opacity(0.08), radius: 16, y: 8)

                    Text("登录脑花")
                        .font(.system(size: 32, weight: .semibold))
                        .foregroundStyle(.black.opacity(0.92))
                        .padding(.top, 26)

                    Text("连接你的脑花设备和智能体，继续上次的对话与文件。")
                        .font(.system(size: 15, weight: .regular))
                        .foregroundStyle(.black.opacity(0.55))
                        .lineSpacing(3)
                        .padding(.top, 8)

                    VStack(spacing: 14) {
                        NativeLoginField(
                            title: "手机号",
                            systemImage: "iphone",
                            text: $viewModel.phone,
                            isSecure: false,
                            keyboardType: .numberPad
                        )
                        .focused($focusedField, equals: .phone)

                        NativeLoginField(
                            title: "密码",
                            systemImage: "lock",
                            text: $viewModel.password,
                            isSecure: true,
                            keyboardType: .default
                        )
                        .focused($focusedField, equals: .password)
                    }
                    .padding(.top, 34)

                    if let error = viewModel.errorMessage {
                        Text(error)
                            .font(.system(size: 13, weight: .medium))
                            .foregroundStyle(Color(red: 0.84, green: 0.12, blue: 0.08))
                            .padding(.top, 12)
                            .transition(.opacity.combined(with: .move(edge: .top)))
                    }

                    Button {
                        focusedField = nil
                        viewModel.login()
                    } label: {
                        ZStack {
                            Text(viewModel.isLoading ? "登录中" : "登录")
                                .font(.system(size: 17, weight: .semibold))
                                .opacity(viewModel.isLoading ? 0 : 1)

                            if viewModel.isLoading {
                                ProgressView()
                                    .tint(.white)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 54)
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(.white)
                    .background(viewModel.canLogin ? Color.black : Color.black.opacity(0.24))
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                    .disabled(!viewModel.canLogin)
                    .padding(.top, 26)

                    Button {
                        viewModel.continueWithComposeLogin()
                    } label: {
                        Text("验证码登录或注册账号")
                            .font(.system(size: 15, weight: .medium))
                            .foregroundStyle(.black.opacity(0.68))
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                    }
                    .buttonStyle(.plain)
                    .padding(.top, 8)

                    Spacer(minLength: 48)
                }
                .padding(.horizontal, 24)
                .frame(maxWidth: 460)
                .frame(maxWidth: .infinity)
            }
            .scrollDismissesKeyboard(.interactively)
        }
    }
}

private struct NativeLoginField: View {
    let title: String
    let systemImage: String
    @Binding var text: String
    let isSecure: Bool
    let keyboardType: UIKeyboardType

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: systemImage)
                .font(.system(size: 17, weight: .medium))
                .foregroundStyle(.black.opacity(0.45))
                .frame(width: 22)

            Group {
                if isSecure {
                    SecureField(title, text: $text)
                        .textContentType(.password)
                } else {
                    TextField(title, text: $text)
                        .textContentType(.telephoneNumber)
                }
            }
            .font(.system(size: 16, weight: .medium))
            .keyboardType(keyboardType)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .foregroundStyle(.black.opacity(0.9))
        }
        .frame(height: 56)
        .padding(.horizontal, 16)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(.black.opacity(0.06), lineWidth: 1)
        }
        .shadow(color: .black.opacity(0.045), radius: 14, y: 8)
    }
}

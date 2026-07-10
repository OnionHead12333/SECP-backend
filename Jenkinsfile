// 私钥：使用 RSA PEM（ssh-keygen -t rsa -b 4096 -m PEM）。整段替换三引号内内容，与 jenkins_rsa 文件一致。
// 勿将含真实私钥的脚本提交到 Git；可只粘到 Jenkins「Pipeline script」。

pipeline {
  agent any

  environment {
    KEY_FILE = "${WORKSPACE}/.ci_gerrit_ssh"
  }

  stages {
    stage('Prepare Gerrit key') {
      steps {
        script {
          def GERRIT_SSH_PRIVATE_KEY = ('''-----BEGIN OPENSSH PRIVATE KEY-----
b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAABFwAAAAdzc2gtcn
NhAAAAAwEAAQAAAQEAxaQmxyZmYdp0UJQjaoggUe/h6PoPzS3gR5jZs4s44IsGQs9do1E/
R8qUSQMu72/m61Ol03D6gdFQp5ibODM+ktOYGP+E/5UH1wOp2NkfeIqf872bC7gc7und4M
Ri1t+UMoK0wotsVEBGNkzlOo6dAJw9xDJ58YnCJWqT6Tjyy4wspmedQlyzsVDWSmT6Whz7
Jq0OG8m3AT0scXNr0Mub7IHtNPGdAehfrAqBbBkUuw40zb6/J2Di1UEfgdCz9HwT6ubfTj
lfC46mztKtHKy/Hn5loz6hWn6nKdpNKuUSUbANiuwxzyEf2IKcZtqNLYGTBIS3BEYqUiky
NQXe10ASQwAAA8gxoOUIMaDlCAAAAAdzc2gtcnNhAAABAQDFpCbHJmZh2nRQlCNqiCBR7+
Ho+g/NLeBHmNmzizjgiwZCz12jUT9HypRJAy7vb+brU6XTcPqB0VCnmJs4Mz6S05gY/4T/
lQfXA6nY2R94ip/zvZsLuBzu6d3gxGLW35QygrTCi2xUQEY2TOU6jp0AnD3EMnnxicIlap
PpOPLLjCymZ51CXLOxUNZKZPpaHPsmrQ4bybcBPSxxc2vQy5vsge008Z0B6F+sCoFsGRS7
DjTNvr8nYOLVQR+B0LP0fBPq5t9OOV8LjqbO0q0crL8efmWjPqFafqcp2k0q5RJRsA2K7D
HPIR/Ygpxm2o0tgZMEhLcERipSKTI1Bd7XQBJDAAAAAwEAAQAAAQEAtBlJXOCZG0I+C89y
FnnFOeUmL27vR0Euxw96EiojwjntkqPz6Ab5ayomxgGom3eVLYwj5/Fj1TgwDwj5KNVJh/
JY/y6Yu23KnxwvgfMYwndQGoQE7UDw3KUwEsniZ3uFSCHjJgR2yq9nS8P2fs3GM/AsOMTm
ajvmNsJP1o6WcPPJJ3HsDjaNjhSIa2aV+7ZSTFYupI7qUukTpqFaZ98o575JIMxBGM2HM8
tRrnMvtx+K6lukNBEE09g0xVRH9pyntb1I4TLUaDaxm2iUDwZwx7Ow9vHWNpv7PSAT0Ntv
CjX8haLBdeg0H8lMOzNftUaZcCVxVnkITBVoY17YAEw8gQAAAIEAthQ0zf+8pRK6W1MpYe
Uxkxn2+lqKmQFs/4e9Bu9gErqhI8EyzSL/h6P0mGASvlRHMCgFddQxIQkQjPBp5Rod2Xvc
HPl7Gmc46m7GB01dCO/sm8EtHphhkIBCitkE/dw3F/F/axreQrivZLa1b0sOel305AD8/F
vHOhUM/1O+G4sAAACBAOJf7j6ZXqOWYFIkJmjCvQct8J5VTZigTUIs+ridOseFMA1DhwE+
y+TVFUqEF2UimFZRedtc4Ze2lo83zwWFhG0OCR5lE8OuVDQqehIuB+7Y6eBM5g2s1fSJy6
F3am755S8PFpqrBcgu5iQ+ISSJ3WEU2GYMZQOn/0YvYCHUQIXpAAAAgQDfgZUqq7mHUsti
Bxw8MONFVqEXj6A8ABP/dkc+mUXt/bvn+LtuBwSclCTBgzKP1Hoc217stWJqImq9tD4vvc
m9fHI3KSge3b8e/c7ipC6jsskOMNnxklofrGOCKvXaxWgk1pPjX1MGsilLhizldtyLabpe
xY+bTw5dftft6vC/SwAAAAoyNTE4MkDUzLqtAQIDBAUGBw==
-----END OPENSSH PRIVATE KEY-----''').stripIndent().trim() + '\n'

          writeFile file: env.KEY_FILE, text: GERRIT_SSH_PRIVATE_KEY
        }
        sh 'chmod 600 "$KEY_FILE"'
      }
    }

    stage('Checkout') {
      steps {
        sh '''
          set -e
          # 与 Gerrit Trigger 事件一致：必须用 GERRIT_PROJECT，勿写死成其它仓库（如 Court_AI）
          PROJECT="${GERRIT_PROJECT:-SCEP-backend}"
          GUSER="${GERRIT_SSH_USER:-23301015}"
          export GIT_SSH_COMMAND="ssh -i $KEY_FILE -o IdentitiesOnly=yes -o StrictHostKeyChecking=no -p 29418"
          rm -rf .git
          git init
          git remote add origin "ssh://${GUSER}@gerrit.lilingkun.com:29418/${PROJECT}"
          echo "Gerrit 仓库: ${PROJECT}（来自 GERRIT_PROJECT，未设置则默认 SCEP-backend）"
          if [ -n "$GERRIT_REFSPEC" ]; then
            REFSPEC="$GERRIT_REFSPEC"
            echo "使用 GERRIT_REFSPEC: $REFSPEC"
          else
            REFSPEC="refs/heads/master"
            echo "GERRIT_REFSPEC 未设置（立即构建），回退: $REFSPEC"
          fi
          git fetch --depth=1 origin "$REFSPEC"
          git checkout -qf FETCH_HEAD
        '''
      }
    }
  }

  post {
    success {
      script {
        // 仅 --message：多数站点默认允许评论；Verified 需在 Gerrit 权限里给本 SSH 用户开放，否则会报 "Applying label Verified is restricted"
        def msg = "Jenkins 构建成功 #${env.BUILD_NUMBER}"
        def ws = env.WORKSPACE
        def msgPath = "${ws}/.ci_gerrit_verify_msg.txt"
        def runPath = "${ws}/.ci_gerrit_vote.sh"
        writeFile file: msgPath, text: msg, encoding: 'UTF-8'
        def bash = '''#!/usr/bin/env bash
set -e
MSG=$(cat '__MSGPATH__')
GUSER="${GERRIT_SSH_USER:-23301015}"
if [ -n "$GERRIT_PATCHSET_REVISION" ] && [ -f "$KEY_FILE" ]; then
  ssh -i "$KEY_FILE" -o IdentitiesOnly=yes -o StrictHostKeyChecking=no -p 29418 \
    "${GUSER}@gerrit.lilingkun.com" \
    "gerrit review --message $(printf %q "$MSG") $(printf %q "$GERRIT_PATCHSET_REVISION")"
fi
rm -f '__MSGPATH__'
'''.replace('__MSGPATH__', msgPath)
        writeFile file: runPath, text: bash, encoding: 'UTF-8'
        sh "chmod +x '${runPath}' && bash '${runPath}'"
      }
    }
    failure {
      script {
        def msg = "Jenkins 构建失败 #${env.BUILD_NUMBER}"
        def ws = env.WORKSPACE
        def msgPath = "${ws}/.ci_gerrit_verify_msg.txt"
        def runPath = "${ws}/.ci_gerrit_vote.sh"
        writeFile file: msgPath, text: msg, encoding: 'UTF-8'
        def bash = '''#!/usr/bin/env bash
set -e
MSG=$(cat '__MSGPATH__')
GUSER="${GERRIT_SSH_USER:-23301015}"
if [ -n "$GERRIT_PATCHSET_REVISION" ] && [ -f "$KEY_FILE" ]; then
  ssh -i "$KEY_FILE" -o IdentitiesOnly=yes -o StrictHostKeyChecking=no -p 29418 \
    "${GUSER}@gerrit.lilingkun.com" \
    "gerrit review --message $(printf %q "$MSG") $(printf %q "$GERRIT_PATCHSET_REVISION")"
fi
rm -f '__MSGPATH__'
'''.replace('__MSGPATH__', msgPath)
        writeFile file: runPath, text: bash, encoding: 'UTF-8'
        sh "chmod +x '${runPath}' && bash '${runPath}'"
      }
    }
    cleanup {
      sh 'rm -f "$KEY_FILE" "${WORKSPACE}/.ci_gerrit_verify_msg.txt" "${WORKSPACE}/.ci_gerrit_vote.sh" || true'
    }
  }
}

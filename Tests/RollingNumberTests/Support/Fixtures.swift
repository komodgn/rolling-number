import Foundation
import XCTest

// Swift mirror of specs/001-rolling-number-display/contracts/fixtures.md.

struct FormatFile: Decodable { let schema: Int; let kind: String; let cases: [FormatCase] }

struct FormatCase: Decodable {
    let id: String
    let spec: String?
    let input: FormatInput
    let expect: FormatExpect
    let platforms: [String]?
    let reason: String?
}

struct FormatInput: Decodable {
    let value: String
    let locale: String
    let format: FormatSpecJSON
    let prefix: String?
    let suffix: String?
}

struct FormatSpecJSON: Decodable {
    let style: String
    let currency: String?
    let minFraction: Int?
    let maxFraction: Int?
    let grouping: Bool?
    let rounding: String?
}

struct FormatExpect: Decodable { let normalized: String; let tokens: [TokenJSON] }

struct TokenJSON: Decodable { let kind: String; let key: String; let text: String; let textMatch: String? }

struct DiffFile: Decodable { let schema: Int; let kind: String; let cases: [DiffCase] }

struct DiffCase: Decodable {
    let id: String
    let spec: String?
    let trend: String?
    let symbols: SymbolsJSON?
    let from: NumberJSON?
    let to: NumberJSON
    let animateFirstDisplay: Bool?
    let expect: DiffExpect
    let platforms: [String]?
    let reason: String?
}

struct SymbolsJSON: Decodable {
    let decimal: String?
    let group: String?
    let minus: [String]?
}

struct NumberJSON: Decodable {
    let text: String
    let value: String
    let prefix: String?
    let suffix: String?
    let formatKey: String?
}

struct DiffExpect: Decodable { let direction: String; let elements: [DiffElementJSON] }

struct DiffElementJSON: Decodable, Equatable {
    let key: String
    let kind: String
    let from: String?
    let to: String?
    let action: String
}

struct SessionFile: Decodable { let schema: Int; let kind: String; let cases: [SessionCase] }

struct SessionCase: Decodable {
    let id: String
    let spec: String?
    let trend: String?
    let symbols: SymbolsJSON?
    let members: [MemberJSON]
    let steps: [StepJSON]
    let expect: [ExpectJSON]
    let platforms: [String]?
    let reason: String?
}

struct MemberJSON: Decodable {
    let id: String
    let label: String?
    let announce: Bool?
    let animateFirstDisplay: Bool?
}

struct StepJSON: Decodable {
    let t: Int64
    let set: [String: NumberJSON]?
    let advance: Bool?
    let disableAnimation: String?
    let enableAnimation: String?
    let reduceMotion: Bool?
    let itemKey: [String: String]?
}

struct ExpectJSON: Decodable, Equatable {
    let event: String?
    let member: String?
    let t: Int64?
    let announce: String?
}

private struct Header: Decodable { let schema: Int; let kind: String }

/// Reads the repo-root `fixtures/` folder located relative to this source file.
enum Fixtures {
    static var root: URL {
        URL(fileURLWithPath: #filePath)            // Tests/RollingNumberTests/Support/Fixtures.swift
            .deletingLastPathComponent()           // Support
            .deletingLastPathComponent()           // RollingNumberTests
            .deletingLastPathComponent()           // Tests
            .deletingLastPathComponent()           // repo root
            .appendingPathComponent("fixtures")
    }

    static func files(_ kind: String) throws -> [(name: String, data: Data)] {
        let dir = root.appendingPathComponent(kind)
        let names = try FileManager.default.contentsOfDirectory(atPath: dir.path)
            .filter { $0.hasSuffix(".json") }
            .sorted()
        return try names.map { name in
            let data = try Data(contentsOf: dir.appendingPathComponent(name))
            let header = try JSONDecoder().decode(Header.self, from: data)
            guard header.schema == 1 else { throw FixtureError("\(name): unknown schema \(header.schema)") }
            guard header.kind == kind else { throw FixtureError("\(name): kind '\(header.kind)' in '\(kind)' folder") }
            return (name, data)
        }
    }

    static func runsOn(_ id: String, _ platforms: [String]?, _ reason: String?) throws -> Bool {
        guard let platforms = platforms else { return true }
        guard let reason = reason, !reason.isEmpty else { throw FixtureError("Fixture '\(id)' restricts platforms without a reason") }
        _ = reason
        return platforms.contains("ios")
    }

    static func format() throws -> [(file: String, case: FormatCase)] {
        try files("format").flatMap { file in
            try JSONDecoder().decode(FormatFile.self, from: file.data).cases
                .filter { try runsOn($0.id, $0.platforms, $0.reason) }
                .map { (file.name, $0) }
        }
    }

    static func diff() throws -> [(file: String, case: DiffCase)] {
        try files("diff").flatMap { file in
            try JSONDecoder().decode(DiffFile.self, from: file.data).cases
                .filter { try runsOn($0.id, $0.platforms, $0.reason) }
                .map { (file.name, $0) }
        }
    }

    static func session() throws -> [(file: String, case: SessionCase)] {
        try files("session").flatMap { file in
            try JSONDecoder().decode(SessionFile.self, from: file.data).cases
                .filter { try runsOn($0.id, $0.platforms, $0.reason) }
                .map { (file.name, $0) }
        }
    }
}

struct FixtureError: Error, CustomStringConvertible {
    let description: String
    init(_ description: String) { self.description = description }
}
